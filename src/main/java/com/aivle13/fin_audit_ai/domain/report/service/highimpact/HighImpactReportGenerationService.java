package com.aivle13.fin_audit_ai.domain.report.service.highimpact;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.entity.DiagnosisAnswerEntity;
import com.aivle13.fin_audit_ai.domain.diagnosis.entity.PreDiagnosisEntity;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.DiagnosisAnswerRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.PreDiagnosisRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.type.DiagnosisQuestion;
import com.aivle13.fin_audit_ai.domain.diagnosis.type.DiagnosisResult;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPersistenceService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.report.HighImpactReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.report.request.HighImpactReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.HighImpactReportResponse;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.diagnosis.PreDiagnosisNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HighImpactReportGenerationService {

    private static final ZoneId REPORT_TIME_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter ASSESSED_AT_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd'T'HH:mm:ssXXX"
            );

    private final AuditRepository auditRepository;
    private final PreDiagnosisRepository preDiagnosisRepository;
    private final DiagnosisAnswerRepository diagnosisAnswerRepository;
    private final HighImpactReportClient reportClient;
    private final ReportPersistenceService reportPersistenceService;

    public Long generateAndSave(
            Long userId,
            Long auditId
    ) {
        AuditEntity audit = auditRepository
                .findByIdAndUser_IdWithModelAndDataset(
                        auditId,
                        userId
                )
                .orElseThrow(AuditNotFoundException::new);

        Long assessmentId = audit.getAssessmentId();

        if (assessmentId == null) {
            throw new PreDiagnosisNotFoundException();
        }

        PreDiagnosisEntity diagnosis = preDiagnosisRepository
                .findByIdAndUser_Id(
                        assessmentId,
                        userId
                )
                .orElseThrow(
                        PreDiagnosisNotFoundException::new
                );

        if (diagnosis.getResult()
                != DiagnosisResult.HIGH_IMPACT) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "고영향으로 확정된 사전진단만 "
                            + "보고서를 생성할 수 있습니다."
            );
        }

        List<DiagnosisAnswerEntity> answers =
                diagnosisAnswerRepository
                        .findAllByDiagnosis_IdOrderByIdAsc(
                                assessmentId
                        );

        HighImpactReportResponse response =
                reportClient.generate(
                        createRequest(
                                audit,
                                diagnosis,
                                answers
                        )
                );

        validateResponse(
                auditId,
                assessmentId,
                response
        );

        Map<ReportFormat, Long> reportIds =
                reportPersistenceService.saveAll(
                        auditId,
                        ReportType.HIGH_IMPACT_REPORT,
                        Map.of(
                                ReportFormat.PDF,
                                response.pdfReportS3Key(),
                                ReportFormat.WORD,
                                response.wordReportS3Key()
                        )
                );

        Long pdfReportId =
                reportIds.get(ReportFormat.PDF);

        if (pdfReportId == null) {
            throw new AuditFailedException();
        }

        return pdfReportId;
    }

    private HighImpactReportRequest createRequest(
            AuditEntity audit,
            PreDiagnosisEntity diagnosis,
            List<DiagnosisAnswerEntity> answers
    ) {
        if (answers == null || answers.isEmpty()) {
            throw new AuditFailedException();
        }

        LocalDateTime assessedAt = diagnosis.getUpdatedAt();

        if (assessedAt == null) {
            throw new AuditFailedException();
        }

        List<HighImpactReportRequest.Answer> reportAnswers =
                answers.stream()
                        .map(this::toReportAnswer)
                        .sorted(
                                Comparator.comparingInt(
                                        answer ->
                                                questionOrder(
                                                        answer.questionCode()
                                                )
                                )
                        )
                        .toList();

        return new HighImpactReportRequest(
                audit.getId(),
                diagnosis.getId(),
                audit.getAuditName(),
                audit.getModel().getModelName(),
                audit.getModel().getVersion(),
                assessedAt
                        .atZone(REPORT_TIME_ZONE)
                        .format(ASSESSED_AT_FORMATTER),
                diagnosis.isConditionMet(),
                diagnosis.getGroupAScore(),
                diagnosis.getGroupBScore(),
                diagnosis.getTotalScore(),
                diagnosis.getResult().name(),
                reportAnswers
        );
    }

    private HighImpactReportRequest.Answer toReportAnswer(
            DiagnosisAnswerEntity answer
    ) {
        DiagnosisQuestion question =
                DiagnosisQuestion.findByCode(
                                answer.getQuestionCode()
                        )
                        .orElseThrow(
                                AuditFailedException::new
                        );

        int expectedScore = answer.isAnswer()
                ? question.getWeight()
                : 0;

        if (answer.getScore() != expectedScore) {
            throw new AuditFailedException();
        }

        return new HighImpactReportRequest.Answer(
                question.getCode(),
                question.getQuestionText(),
                question.getStage(),
                question.getGroup(),
                answer.isAnswer(),
                question.getWeight(),
                answer.getScore()
        );
    }

    private int questionOrder(String questionCode) {
        return DiagnosisQuestion.findByCode(questionCode)
                .map(Enum::ordinal)
                .orElseThrow(AuditFailedException::new);
    }

    private void validateResponse(
            Long auditId,
            Long assessmentId,
            HighImpactReportResponse response
    ) {
        if (response == null
                || !auditId.equals(response.auditId())
                || !assessmentId.equals(
                response.assessmentId()
        )
                || response.pdfReportS3Key() == null
                || response.pdfReportS3Key().isBlank()
                || response.wordReportS3Key() == null
                || response.wordReportS3Key().isBlank()) {
            throw new AuditFailedException();
        }
    }
}
