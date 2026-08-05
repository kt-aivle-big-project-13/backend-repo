package com.aivle13.fin_audit_ai.domain.diagnosis.service;

import com.aivle13.fin_audit_ai.domain.diagnosis.dto.request.PreDiagnosisQuantitativeRequest;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.request.PreDiagnosisRequest;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.response.PreDiagnosisResponse;
import com.aivle13.fin_audit_ai.domain.diagnosis.entity.DiagnosisAnswerEntity;
import com.aivle13.fin_audit_ai.domain.diagnosis.entity.PreDiagnosisEntity;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.DiagnosisAnswerRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.PreDiagnosisRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.type.DiagnosisResult;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.diagnosis.PreDiagnosisNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.user.common.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisService {

    private static final Set<String> GATE_QUESTION_CODES = Set.of("GATE_01", "GATE_02");
    private static final Set<String> GROUP_A_QUESTION_CODES = Set.of("A_01", "A_02", "A_03");
    private static final Set<String> GROUP_B_QUESTION_CODES = Set.of("B_01", "B_02", "B_03");
    private static final Set<String> QUANTITATIVE_QUESTION_CODES = Set.of("A_01", "A_02", "A_03", "B_01", "B_02", "B_03");
    private static final int GROUP_A_SCORE = 2;
    private static final int GROUP_B_SCORE = 1;
    private static final int HIGH_IMPACT_THRESHOLD = 4;

    private final UserRepository userRepository;
    private final PreDiagnosisRepository preDiagnosisRepository;
    private final DiagnosisAnswerRepository diagnosisAnswerRepository;

    @Transactional
    public PreDiagnosisResponse start(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        PreDiagnosisEntity diagnosis = PreDiagnosisEntity.create(user, DiagnosisResult.IN_PROGRESS);
        return toResponse(preDiagnosisRepository.save(diagnosis));
    }

    @Transactional
    public void linkModel(Long assessmentId, Long userId, AiModelEntity model) {
        PreDiagnosisEntity diagnosis = preDiagnosisRepository.findById(assessmentId)
                .orElseThrow(PreDiagnosisNotFoundException::new);

        if (!diagnosis.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Cannot link model to another user's pre-diagnosis.");
        }

        diagnosis.linkModel(model);
    }

    @Transactional
    public PreDiagnosisResponse diagnoseQualitative(Long assessmentId, PreDiagnosisRequest request) {
        validateGateAnswers(request.answers());

        PreDiagnosisEntity diagnosis = preDiagnosisRepository.findById(assessmentId)
                .orElseThrow(PreDiagnosisNotFoundException::new);

        if (diagnosis.getResult() != DiagnosisResult.IN_PROGRESS) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "Qualitative diagnosis is only available for an in-progress pre-diagnosis."
            );
        }

        boolean conditionMet = request.answers().stream()
                .anyMatch(PreDiagnosisRequest.AnswerDto::answer);

        DiagnosisResult result = conditionMet
                ? DiagnosisResult.HIGH_IMPACT
                : DiagnosisResult.NEEDS_QUANTITATIVE;

        diagnosis.updateQualitativeResult(conditionMet, result);

        List<DiagnosisAnswerEntity> answers = request.answers().stream()
                .map(answer -> DiagnosisAnswerEntity.of(
                        diagnosis,
                        answer.questionCode(),
                        answer.answer(),
                        0
                ))
                .toList();
        diagnosisAnswerRepository.saveAll(answers);

        return toResponse(diagnosis);
    }

    @Transactional
    public PreDiagnosisResponse diagnoseQuantitative(Long assessmentId, PreDiagnosisQuantitativeRequest request) {
        validateQuantitativeAnswers(request.answers());

        PreDiagnosisEntity diagnosis = preDiagnosisRepository.findById(assessmentId)
                .orElseThrow(PreDiagnosisNotFoundException::new);

        if (diagnosis.getResult() != DiagnosisResult.NEEDS_QUANTITATIVE) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "Quantitative assessment is only available after passing the gate step."
            );
        }

        int groupAScore = calculateGroupScore(request.answers(), GROUP_A_QUESTION_CODES, GROUP_A_SCORE);
        int groupBScore = calculateGroupScore(request.answers(), GROUP_B_QUESTION_CODES, GROUP_B_SCORE);
        int totalScore = groupAScore + groupBScore;

        DiagnosisResult result = totalScore >= HIGH_IMPACT_THRESHOLD
                ? DiagnosisResult.HIGH_IMPACT
                : DiagnosisResult.NOT_APPLICABLE;

        diagnosis.updateQuantitativeResult(groupAScore, groupBScore, totalScore, result);

        List<DiagnosisAnswerEntity> answers = request.answers().stream()
                .map(answer -> DiagnosisAnswerEntity.of(
                        diagnosis,
                        answer.questionCode(),
                        answer.answer(),
                        calculateAnswerScore(answer.questionCode(), answer.answer())
                ))
                .toList();
        diagnosisAnswerRepository.saveAll(answers);

        return toResponse(diagnosis);
    }

    public PreDiagnosisResponse getResult(Long assessmentId) {
        PreDiagnosisEntity diagnosis = preDiagnosisRepository.findById(assessmentId)
                .orElseThrow(PreDiagnosisNotFoundException::new);

        return toResponse(diagnosis);
    }

    private void validateGateAnswers(List<PreDiagnosisRequest.AnswerDto> answers) {
        Set<String> questionCodes = new HashSet<>();

        for (PreDiagnosisRequest.AnswerDto answer : answers) {
            if (!GATE_QUESTION_CODES.contains(answer.questionCode())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "GATE_01, GATE_02 questions only can be submitted.");
            }

            if (!questionCodes.add(answer.questionCode())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Duplicated gate diagnosis question.");
            }
        }

        if (!questionCodes.containsAll(GATE_QUESTION_CODES)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "GATE_01, GATE_02 questions must all be checked.");
        }
    }

    private void validateQuantitativeAnswers(List<PreDiagnosisQuantitativeRequest.AnswerDto> answers) {
        Set<String> questionCodes = new HashSet<>();

        for (PreDiagnosisQuantitativeRequest.AnswerDto answer : answers) {
            if (!QUANTITATIVE_QUESTION_CODES.contains(answer.questionCode())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "A_01~A_03, B_01~B_03 questions only can be submitted.");
            }

            if (!questionCodes.add(answer.questionCode())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Duplicated quantitative diagnosis question.");
            }
        }

        if (!questionCodes.containsAll(QUANTITATIVE_QUESTION_CODES)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "A_01~A_03, B_01~B_03 questions must all be checked.");
        }
    }

    private int calculateGroupScore(
            List<PreDiagnosisQuantitativeRequest.AnswerDto> answers,
            Set<String> groupQuestionCodes,
            int score
    ) {
        return answers.stream()
                .filter(answer -> groupQuestionCodes.contains(answer.questionCode()))
                .filter(PreDiagnosisQuantitativeRequest.AnswerDto::answer)
                .mapToInt(answer -> score)
                .sum();
    }

    private int calculateAnswerScore(String questionCode, boolean answer) {
        if (!answer) {
            return 0;
        }

        if (GROUP_A_QUESTION_CODES.contains(questionCode)) {
            return GROUP_A_SCORE;
        }

        return GROUP_B_SCORE;
    }

    private PreDiagnosisResponse toResponse(PreDiagnosisEntity diagnosis) {
        return new PreDiagnosisResponse(
                diagnosis.getId(),
                diagnosis.getUser().getId(),
                diagnosis.getModel() == null ? null : diagnosis.getModel().getId(),
                diagnosis.isConditionMet(),
                diagnosis.getGroupAScore(),
                diagnosis.getGroupBScore(),
                diagnosis.getTotalScore(),
                diagnosis.getResult()
        );
    }
}
