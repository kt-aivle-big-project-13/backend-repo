package com.aivle13.fin_audit_ai.domain.diagnosis.service;

import com.aivle13.fin_audit_ai.domain.diagnosis.dto.request.PreDiagnosisQuantitativeRequest;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.request.PreDiagnosisRequest;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.response.PreDiagnosisResponse;
import com.aivle13.fin_audit_ai.domain.diagnosis.entity.PreDiagnosisEntity;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.DiagnosisAnswerRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.PreDiagnosisRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.type.DiagnosisResult;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.diagnosis.PreDiagnosisNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.user.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiagnosisServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PreDiagnosisRepository preDiagnosisRepository;

    @Mock
    private DiagnosisAnswerRepository diagnosisAnswerRepository;

    @InjectMocks
    private DiagnosisService diagnosisService;

    private UserEntity user(Long id) {
        UserEntity user = UserEntity.create("홍길동", "테스트기관", "user" + id + "@example.com", "hash", UserRole.AUDITOR);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private PreDiagnosisEntity diagnosis(Long id, UserEntity owner, DiagnosisResult result) {
        PreDiagnosisEntity diagnosis = PreDiagnosisEntity.create(owner, result);
        ReflectionTestUtils.setField(diagnosis, "id", id);
        return diagnosis;
    }

    @Test
    void 사전진단을_시작하면_IN_PROGRESS_상태로_생성된다() {
        UserEntity user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(preDiagnosisRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        PreDiagnosisResponse response = diagnosisService.start(1L);

        ArgumentCaptor<PreDiagnosisEntity> captor = ArgumentCaptor.forClass(PreDiagnosisEntity.class);
        verify(preDiagnosisRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(DiagnosisResult.IN_PROGRESS);
        assertThat(response.result()).isEqualTo(DiagnosisResult.IN_PROGRESS);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.modelId()).isNull();
    }

    @Test
    void 존재하지_않는_사용자면_시작시_예외() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> diagnosisService.start(1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void 모델을_사전진단에_연결한다() {
        UserEntity owner = user(1L);
        PreDiagnosisEntity diagnosis = diagnosis(10L, owner, DiagnosisResult.IN_PROGRESS);
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.of(diagnosis));
        AiModelEntity model = mock(AiModelEntity.class);

        diagnosisService.linkModel(10L, 1L, model);

        assertThat(diagnosis.getModel()).isEqualTo(model);
    }

    @Test
    void 다른_사용자의_사전진단에_모델_연결시_예외() {
        UserEntity owner = user(1L);
        PreDiagnosisEntity diagnosis = diagnosis(10L, owner, DiagnosisResult.IN_PROGRESS);
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.of(diagnosis));
        AiModelEntity model = mock(AiModelEntity.class);

        assertThatThrownBy(() -> diagnosisService.linkModel(10L, 2L, model))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    void GATE_문항_중_하나라도_해당되면_HIGH_IMPACT다() {
        PreDiagnosisEntity diagnosis = diagnosis(10L, user(1L), DiagnosisResult.IN_PROGRESS);
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.of(diagnosis));
        PreDiagnosisRequest request = new PreDiagnosisRequest(List.of(
                new PreDiagnosisRequest.AnswerDto("GATE_01", true),
                new PreDiagnosisRequest.AnswerDto("GATE_02", false)
        ));

        PreDiagnosisResponse response = diagnosisService.diagnoseQualitative(10L, request);

        assertThat(response.conditionMet()).isTrue();
        assertThat(response.result()).isEqualTo(DiagnosisResult.HIGH_IMPACT);
        verify(diagnosisAnswerRepository).saveAll(any());
    }

    @Test
    void GATE_문항이_모두_해당없음이면_정량진단_필요_상태다() {
        PreDiagnosisEntity diagnosis = diagnosis(10L, user(1L), DiagnosisResult.IN_PROGRESS);
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.of(diagnosis));
        PreDiagnosisRequest request = new PreDiagnosisRequest(List.of(
                new PreDiagnosisRequest.AnswerDto("GATE_01", false),
                new PreDiagnosisRequest.AnswerDto("GATE_02", false)
        ));

        PreDiagnosisResponse response = diagnosisService.diagnoseQualitative(10L, request);

        assertThat(response.conditionMet()).isFalse();
        assertThat(response.result()).isEqualTo(DiagnosisResult.NEEDS_QUANTITATIVE);
    }

    @Test
    void GATE_문항이_아닌_코드가_섞이면_예외() {
        PreDiagnosisRequest request = new PreDiagnosisRequest(List.of(
                new PreDiagnosisRequest.AnswerDto("GATE_01", true),
                new PreDiagnosisRequest.AnswerDto("A_01", false)
        ));

        assertThatThrownBy(() -> diagnosisService.diagnoseQualitative(10L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void GATE_문항이_중복되면_예외() {
        PreDiagnosisRequest request = new PreDiagnosisRequest(List.of(
                new PreDiagnosisRequest.AnswerDto("GATE_01", true),
                new PreDiagnosisRequest.AnswerDto("GATE_01", false)
        ));

        assertThatThrownBy(() -> diagnosisService.diagnoseQualitative(10L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void IN_PROGRESS가_아니면_정성진단_제출시_예외() {
        PreDiagnosisEntity diagnosis = diagnosis(10L, user(1L), DiagnosisResult.HIGH_IMPACT);
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.of(diagnosis));
        PreDiagnosisRequest request = new PreDiagnosisRequest(List.of(
                new PreDiagnosisRequest.AnswerDto("GATE_01", true),
                new PreDiagnosisRequest.AnswerDto("GATE_02", false)
        ));

        assertThatThrownBy(() -> diagnosisService.diagnoseQualitative(10L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 가중점수_합이_임계값_이상이면_HIGH_IMPACT로_확정된다() {
        PreDiagnosisEntity diagnosis = diagnosis(10L, user(1L), DiagnosisResult.NEEDS_QUANTITATIVE);
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.of(diagnosis));
        PreDiagnosisQuantitativeRequest request = new PreDiagnosisQuantitativeRequest(List.of(
                new PreDiagnosisQuantitativeRequest.AnswerDto("A_01", true),
                new PreDiagnosisQuantitativeRequest.AnswerDto("A_02", true),
                new PreDiagnosisQuantitativeRequest.AnswerDto("A_03", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("B_01", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("B_02", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("B_03", false)
        ));

        PreDiagnosisResponse response = diagnosisService.diagnoseQuantitative(10L, request);

        assertThat(response.groupAScore()).isEqualTo(4);
        assertThat(response.groupBScore()).isEqualTo(0);
        assertThat(response.totalScore()).isEqualTo(4);
        assertThat(response.result()).isEqualTo(DiagnosisResult.HIGH_IMPACT);
    }

    @Test
    void 가중점수_합이_임계값_미만이면_NOT_APPLICABLE로_확정된다() {
        PreDiagnosisEntity diagnosis = diagnosis(10L, user(1L), DiagnosisResult.NEEDS_QUANTITATIVE);
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.of(diagnosis));
        PreDiagnosisQuantitativeRequest request = new PreDiagnosisQuantitativeRequest(List.of(
                new PreDiagnosisQuantitativeRequest.AnswerDto("A_01", true),
                new PreDiagnosisQuantitativeRequest.AnswerDto("A_02", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("A_03", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("B_01", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("B_02", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("B_03", false)
        ));

        PreDiagnosisResponse response = diagnosisService.diagnoseQuantitative(10L, request);

        assertThat(response.totalScore()).isEqualTo(2);
        assertThat(response.result()).isEqualTo(DiagnosisResult.NOT_APPLICABLE);
    }

    @Test
    void NEEDS_QUANTITATIVE가_아니면_정량진단_제출시_예외() {
        PreDiagnosisEntity diagnosis = diagnosis(10L, user(1L), DiagnosisResult.IN_PROGRESS);
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.of(diagnosis));
        PreDiagnosisQuantitativeRequest request = new PreDiagnosisQuantitativeRequest(List.of(
                new PreDiagnosisQuantitativeRequest.AnswerDto("A_01", true),
                new PreDiagnosisQuantitativeRequest.AnswerDto("A_02", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("A_03", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("B_01", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("B_02", false),
                new PreDiagnosisQuantitativeRequest.AnswerDto("B_03", false)
        ));

        assertThatThrownBy(() -> diagnosisService.diagnoseQuantitative(10L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void 사전진단_결과를_조회한다() {
        PreDiagnosisEntity diagnosis = diagnosis(10L, user(1L), DiagnosisResult.HIGH_IMPACT);
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.of(diagnosis));

        PreDiagnosisResponse response = diagnosisService.getResult(10L);

        assertThat(response.assessmentId()).isEqualTo(10L);
        assertThat(response.result()).isEqualTo(DiagnosisResult.HIGH_IMPACT);
    }

    @Test
    void 존재하지_않는_사전진단_조회시_예외() {
        given(preDiagnosisRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> diagnosisService.getResult(10L))
                .isInstanceOf(PreDiagnosisNotFoundException.class);
    }
}
