package com.aivle13.fin_audit_ai.domain.audit.service.core;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.core.AuditRetryResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.core.AuditSummaryResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.event.AuditStartedEvent;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.model.AuditAlreadyInProgressException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCancellableException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotRetryableException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;
    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private XaiResultRepository xaiResultRepository;
    @Mock
    private FairnessResultRepository fairnessResultRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private UserEntity user;

    @InjectMocks
    private AuditService auditService;

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 10L;

    private AiModelEntity aiModel() {
        return AiModelEntity.create(null, "my-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING, "models/model.json", "1.0.0");
    }

    private DatasetEntity dataset(AiModelEntity model) {
        return DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/audit-key.csv", 100, "age,gender,income");
    }

    @Test
    void 모델과_데이터셋을_참조하는_AuditEntity를_PENDING_상태로_저장한다() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(auditRepository.save(any(AuditEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        auditService.create(USER_ID, model, dataset, "audit-name", "age,gender", 7L,
                ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);

        ArgumentCaptor<AuditEntity> captor = ArgumentCaptor.forClass(AuditEntity.class);
        verify(auditRepository).save(captor.capture());
        AuditEntity saved = captor.getValue();
        assertThat(saved.getModel()).isEqualTo(model);
        assertThat(saved.getDataset()).isEqualTo(dataset);
        assertThat(saved.getAuditName()).isEqualTo("audit-name");
        assertThat(saved.getSensitiveFeatures()).isEqualTo("age,gender");
        assertThat(saved.getAssessmentId()).isEqualTo(7L);
        assertThat(saved.getThresholdMethod()).isEqualTo(ThresholdMethod.MANUAL);
        assertThat(saved.getManualThreshold()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
        assertThat(saved.getStatus()).isEqualTo(AuditStatus.PENDING);
        assertThat(saved.getValidationDataset()).isNull();
    }

    @Test
    void assessmentId가_없으면_null로_저장한다() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(auditRepository.save(any(AuditEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        auditService.create(USER_ID, model, dataset, "audit-name", "age,gender", null,
                ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);

        ArgumentCaptor<AuditEntity> captor = ArgumentCaptor.forClass(AuditEntity.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getAssessmentId()).isNull();
    }

    @Test
    void 검증_데이터셋을_지정하면_그대로_저장한다() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        DatasetEntity validationDataset = DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/valid.csv", 50, "age,gender,income");
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(auditRepository.save(any(AuditEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        auditService.create(USER_ID, model, dataset, "audit-name", "age,gender", null,
                ThresholdMethod.VALIDATION_DATASET, BigDecimal.valueOf(0.9), null, validationDataset);

        ArgumentCaptor<AuditEntity> captor = ArgumentCaptor.forClass(AuditEntity.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getValidationDataset()).isEqualTo(validationDataset);
    }

    @Test
    void 사용자의_감사_목록을_요약해서_반환한다() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(auditRepository.save(any(AuditEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AuditEntity audit = auditService.create(USER_ID, model, dataset, "audit-name", "age,gender", null,
                ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        audit.markInProgress();
        audit.moveToStep(3);
        audit.complete(4, AuditStatus.COMPLIANT);

        given(auditRepository.findByUser_IdOrderByCreatedAtDescIdDesc(USER_ID))
                .willReturn(List.of(audit));

        List<AuditSummaryResponse> result = auditService.list(USER_ID);

        assertThat(result).hasSize(1);
        AuditSummaryResponse summary = result.get(0);
        assertThat(summary.modelName()).isEqualTo("my-model");
        assertThat(summary.modelGroupId()).isEqualTo(model.getModelGroupId());
        assertThat(summary.version()).isEqualTo("1.0.0");
        assertThat(summary.status()).isEqualTo(AuditStatus.COMPLIANT);
        assertThat(summary.currentStep()).isEqualTo(4);
        assertThat(summary.completedAt()).isNotNull();
    }

    @Test
    void 대기_중인_감사를_취소하면_CANCELLED_상태가_된다() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        AuditEntity audit = AuditEntity.create(model, dataset, user, "audit-name", "age,gender", null,
                ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        auditService.cancel(AUDIT_ID, USER_ID);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.CANCELLED);
    }

    @Test
    void 진행_중인_감사도_취소할_수_있다() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        AuditEntity audit = AuditEntity.create(model, dataset, user, "audit-name", "age,gender", null,
                ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        audit.markInProgress();
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        auditService.cancel(AUDIT_ID, USER_ID);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.CANCELLED);
    }

    @Test
    void 이미_완료된_감사는_취소할_수_없다() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        AuditEntity audit = AuditEntity.create(model, dataset, user, "audit-name", "age,gender", null,
                ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        audit.markInProgress();
        audit.moveToStep(3);
        audit.complete(4, AuditStatus.COMPLIANT);
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        assertThatThrownBy(() -> auditService.cancel(AUDIT_ID, USER_ID))
                .isInstanceOf(AuditNotCancellableException.class);
    }

    @Test
    void 존재하지_않는_감사를_취소하면_예외가_발생한다() {
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.cancel(AUDIT_ID, USER_ID))
                .isInstanceOf(AuditNotFoundException.class);
    }

    private AuditEntity failedAudit() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        AuditEntity audit = AuditEntity.create(model, dataset, user, "audit-name", "age,gender", null,
                ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        audit.markInProgress();
        audit.moveToStep(3);
        audit.markFailed();
        return audit;
    }

    @Test
    void 실패한_감사를_재시도하면_PENDING으로_리셋되고_분석이_재발행된다() {
        AuditEntity audit = failedAudit();
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(aiModelRepository.findByIdAndUser_IdForUpdate(audit.getModel().getId(), USER_ID))
                .willReturn(Optional.of(audit.getModel()));

        AuditRetryResponse response = auditService.retry(AUDIT_ID, USER_ID);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.PENDING);
        assertThat(audit.getCurrentStep()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.retriedAt()).isNotNull();
        verify(xaiResultRepository).deleteAllByAudit_Id(AUDIT_ID);
        verify(fairnessResultRepository).deleteAllByAudit_Id(AUDIT_ID);

        ArgumentCaptor<AuditStartedEvent> eventCaptor = ArgumentCaptor.forClass(AuditStartedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().auditId()).isEqualTo(AUDIT_ID);
        // 재시도 때마다 세대가 올라가야, 취소 전 실행에서 뒤늦게 도착하는 콜백을
        // AuditProgressService가 다른 세대로 구분해 무시할 수 있다.
        assertThat(eventCaptor.getValue().generation()).isEqualTo(audit.getGeneration());
        assertThat(audit.getGeneration()).isEqualTo(1);
    }

    @Test
    void 재시도할_때마다_세대가_증가한다() {
        AuditEntity audit = failedAudit();
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(aiModelRepository.findByIdAndUser_IdForUpdate(audit.getModel().getId(), USER_ID))
                .willReturn(Optional.of(audit.getModel()));

        auditService.retry(AUDIT_ID, USER_ID);
        audit.markFailed();
        auditService.retry(AUDIT_ID, USER_ID);

        ArgumentCaptor<AuditStartedEvent> eventCaptor = ArgumentCaptor.forClass(AuditStartedEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues().get(0).generation()).isEqualTo(1);
        assertThat(eventCaptor.getAllValues().get(1).generation()).isEqualTo(2);
    }

    @Test
    void 취소된_감사도_재시도할_수_있다() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        AuditEntity audit = AuditEntity.create(model, dataset, user, "audit-name", "age,gender", null,
                ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        audit.markInProgress();
        audit.cancel();
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(aiModelRepository.findByIdAndUser_IdForUpdate(model.getId(), USER_ID))
                .willReturn(Optional.of(model));

        AuditRetryResponse response = auditService.retry(AUDIT_ID, USER_ID);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.PENDING);
        assertThat(response.status()).isEqualTo("PENDING");
        verify(xaiResultRepository).deleteAllByAudit_Id(AUDIT_ID);
        verify(fairnessResultRepository).deleteAllByAudit_Id(AUDIT_ID);
        verify(eventPublisher).publishEvent(any(AuditStartedEvent.class));
    }

    @Test
    void 같은_모델에_이미_활성_감사가_있으면_재시도할_수_없다() {
        AuditEntity audit = failedAudit();
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(aiModelRepository.findByIdAndUser_IdForUpdate(audit.getModel().getId(), USER_ID))
                .willReturn(Optional.of(audit.getModel()));
        given(auditRepository.existsByModel_IdAndStatusIn(
                audit.getModel().getId(), List.of(AuditStatus.PENDING, AuditStatus.IN_PROGRESS)))
                .willReturn(true);

        assertThatThrownBy(() -> auditService.retry(AUDIT_ID, USER_ID))
                .isInstanceOf(AuditAlreadyInProgressException.class);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.FAILED);
        verify(xaiResultRepository, never()).deleteAllByAudit_Id(AUDIT_ID);
        verify(fairnessResultRepository, never()).deleteAllByAudit_Id(AUDIT_ID);
        verify(eventPublisher, never()).publishEvent(any(AuditStartedEvent.class));
    }

    @Test
    void 재시도_시_모델_row를_잠가서_동시_활성화를_막는다() {
        AuditEntity audit = failedAudit();
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(aiModelRepository.findByIdAndUser_IdForUpdate(audit.getModel().getId(), USER_ID))
                .willReturn(Optional.of(audit.getModel()));

        auditService.retry(AUDIT_ID, USER_ID);

        // existsByModel_IdAndStatusIn(같은 모델에 대한 동시 재시도/시작 여부 확인) 전에
        // 모델 row를 먼저 잠가서, 두 요청이 동시에 이 체크를 통과하지 못하게 한다.
        verify(aiModelRepository).findByIdAndUser_IdForUpdate(audit.getModel().getId(), USER_ID);
    }

    @Test
    void 모델을_잠글_수_없으면_재시도가_거부된다() {
        AuditEntity audit = failedAudit();
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(aiModelRepository.findByIdAndUser_IdForUpdate(audit.getModel().getId(), USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.retry(AUDIT_ID, USER_ID))
                .isInstanceOf(ModelNotFoundException.class);

        verify(xaiResultRepository, never()).deleteAllByAudit_Id(AUDIT_ID);
        verify(fairnessResultRepository, never()).deleteAllByAudit_Id(AUDIT_ID);
        verify(eventPublisher, never()).publishEvent(any(AuditStartedEvent.class));
    }

    @Test
    void FAILED가_아닌_감사는_재시도할_수_없다() {
        AiModelEntity model = aiModel();
        DatasetEntity dataset = dataset(model);
        AuditEntity audit = AuditEntity.create(model, dataset, user, "audit-name", "age,gender", null,
                ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
        audit.markInProgress();
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        assertThatThrownBy(() -> auditService.retry(AUDIT_ID, USER_ID))
                .isInstanceOf(AuditNotRetryableException.class);

        verify(xaiResultRepository, never()).deleteAllByAudit_Id(AUDIT_ID);
        verify(fairnessResultRepository, never()).deleteAllByAudit_Id(AUDIT_ID);
        verify(eventPublisher, never()).publishEvent(any(AuditStartedEvent.class));
    }

    @Test
    void 존재하지_않는_감사를_재시도하면_예외가_발생한다() {
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.retry(AUDIT_ID, USER_ID))
                .isInstanceOf(AuditNotFoundException.class);
    }
}
