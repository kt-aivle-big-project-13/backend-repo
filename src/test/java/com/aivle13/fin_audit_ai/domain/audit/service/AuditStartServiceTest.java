package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditStartRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditStartResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.exception.model.AuditAlreadyInProgressException;
import com.aivle13.fin_audit_ai.global.exception.model.DatasetNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.SensitiveAttributesNotSelectedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditStartServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long MODEL_ID = 100L;
    private static final Long DATASET_ID = 200L;

    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private DatasetRepository datasetRepository;
    @Mock
    private AuditRepository auditRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private UserEntity ownerUser;
    @Mock
    private UserEntity otherUser;
    @Mock
    private AuditEntity audit;

    @InjectMocks
    private AuditStartService auditStartService;

    private AiModelEntity newModel(UserEntity owner) {
        return AiModelEntity.create(owner, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model-v2.json", "2.0.0");
    }

    private AiModelEntity sameGroupModel(AiModelEntity original, UserEntity owner) {
        return AiModelEntity.create(owner, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model-v1.json", "1.0.0", original.getModelGroupId());
    }

    private DatasetEntity auditDataset(AiModelEntity model) {
        DatasetEntity dataset = DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/audit.csv", 100, "age,gender,income");
        dataset.updateSensitiveAttributes("age,gender");
        return dataset;
    }

    private AuditStartRequest request() {
        return new AuditStartRequest(MODEL_ID, DATASET_ID, null, "audit-name", ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5));
    }

    @Test
    void 같은_모델의_데이터셋으로_감사를_시작한다() {
        AiModelEntity model = newModel(ownerUser);
        DatasetEntity dataset = auditDataset(model);
        given(ownerUser.getId()).willReturn(USER_ID);
        given(aiModelRepository.findByIdAndUser_IdForUpdate(MODEL_ID, USER_ID)).willReturn(Optional.of(model));
        given(datasetRepository.findById(DATASET_ID)).willReturn(Optional.of(dataset));
        given(auditRepository.existsByModel_IdAndStatusIn(any(), any())).willReturn(false);
        given(auditService.create(any(), any(), any(), any(), any(), any(), any(), any(), any())).willReturn(audit);
        given(audit.getId()).willReturn(1L);
        given(audit.getStatus()).willReturn(AuditStatus.PENDING);

        AuditStartResponse response = auditStartService.start(USER_ID, request());

        assertThat(response.auditId()).isEqualTo(1L);
        assertThat(dataset.isAudited()).isTrue();
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void 다른_버전_모델의_데이터셋도_같은_모델_계열이면_재사용한다() {
        AiModelEntity currentModel = newModel(ownerUser);
        AiModelEntity previousVersionModel = sameGroupModel(currentModel, ownerUser);
        DatasetEntity dataset = auditDataset(previousVersionModel);
        given(ownerUser.getId()).willReturn(USER_ID);
        given(aiModelRepository.findByIdAndUser_IdForUpdate(MODEL_ID, USER_ID)).willReturn(Optional.of(currentModel));
        given(datasetRepository.findById(DATASET_ID)).willReturn(Optional.of(dataset));
        given(auditRepository.existsByModel_IdAndStatusIn(any(), any())).willReturn(false);
        given(auditService.create(any(), any(), any(), any(), any(), any(), any(), any(), any())).willReturn(audit);
        given(audit.getId()).willReturn(1L);
        given(audit.getStatus()).willReturn(AuditStatus.PENDING);

        AuditStartResponse response = auditStartService.start(USER_ID, request());

        assertThat(response.auditId()).isEqualTo(1L);
        ArgumentCaptor<DatasetEntity> datasetCaptor = ArgumentCaptor.forClass(DatasetEntity.class);
        verify(auditService).create(any(), any(), datasetCaptor.capture(), any(), any(), any(), any(), any(), any());
        assertThat(datasetCaptor.getValue()).isEqualTo(dataset);
    }

    @Test
    void 다른_사용자의_데이터셋이면_거부한다() {
        AiModelEntity currentModel = newModel(ownerUser);
        AiModelEntity otherUsersModel = sameGroupModel(currentModel, otherUser);
        DatasetEntity dataset = auditDataset(otherUsersModel);
        given(otherUser.getId()).willReturn(OTHER_USER_ID);
        given(aiModelRepository.findByIdAndUser_IdForUpdate(MODEL_ID, USER_ID)).willReturn(Optional.of(currentModel));
        given(datasetRepository.findById(DATASET_ID)).willReturn(Optional.of(dataset));

        assertThatThrownBy(() -> auditStartService.start(USER_ID, request()))
                .isInstanceOf(DatasetNotFoundException.class);
    }

    @Test
    void 같은_사용자여도_다른_모델_계열이면_거부한다() {
        AiModelEntity currentModel = newModel(ownerUser);
        AiModelEntity unrelatedModel = newModel(ownerUser);
        DatasetEntity dataset = auditDataset(unrelatedModel);
        given(ownerUser.getId()).willReturn(USER_ID);
        given(aiModelRepository.findByIdAndUser_IdForUpdate(MODEL_ID, USER_ID)).willReturn(Optional.of(currentModel));
        given(datasetRepository.findById(DATASET_ID)).willReturn(Optional.of(dataset));

        assertThatThrownBy(() -> auditStartService.start(USER_ID, request()))
                .isInstanceOf(DatasetNotFoundException.class);
    }

    @Test
    void 데이터셋_purpose가_AUDIT이_아니면_거부한다() {
        AiModelEntity model = newModel(ownerUser);
        DatasetEntity dataset = auditDataset(model);
        dataset.markAsValidation();
        given(ownerUser.getId()).willReturn(USER_ID);
        given(aiModelRepository.findByIdAndUser_IdForUpdate(MODEL_ID, USER_ID)).willReturn(Optional.of(model));
        given(datasetRepository.findById(DATASET_ID)).willReturn(Optional.of(dataset));

        assertThatThrownBy(() -> auditStartService.start(USER_ID, request()))
                .isInstanceOf(DatasetNotFoundException.class);
    }

    @Test
    void 민감정보가_선택되지_않았으면_거부한다() {
        AiModelEntity model = newModel(ownerUser);
        DatasetEntity dataset = DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/audit.csv", 100, "age,gender,income");
        given(ownerUser.getId()).willReturn(USER_ID);
        given(aiModelRepository.findByIdAndUser_IdForUpdate(MODEL_ID, USER_ID)).willReturn(Optional.of(model));
        given(datasetRepository.findById(DATASET_ID)).willReturn(Optional.of(dataset));

        assertThatThrownBy(() -> auditStartService.start(USER_ID, request()))
                .isInstanceOf(SensitiveAttributesNotSelectedException.class);
    }

    @Test
    void 이미_진행_중인_감사가_있으면_거부한다() {
        AiModelEntity model = newModel(ownerUser);
        DatasetEntity dataset = auditDataset(model);
        given(ownerUser.getId()).willReturn(USER_ID);
        given(aiModelRepository.findByIdAndUser_IdForUpdate(MODEL_ID, USER_ID)).willReturn(Optional.of(model));
        given(datasetRepository.findById(DATASET_ID)).willReturn(Optional.of(dataset));
        given(auditRepository.existsByModel_IdAndStatusIn(any(), any())).willReturn(true);

        assertThatThrownBy(() -> auditStartService.start(USER_ID, request()))
                .isInstanceOf(AuditAlreadyInProgressException.class);
    }

    @Test
    void 모델이_존재하지_않으면_예외가_발생한다() {
        given(aiModelRepository.findByIdAndUser_IdForUpdate(MODEL_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> auditStartService.start(USER_ID, request()))
                .isInstanceOf(ModelNotFoundException.class);
    }

    @Test
    void 데이터셋이_존재하지_않으면_예외가_발생한다() {
        AiModelEntity model = newModel(ownerUser);
        given(aiModelRepository.findByIdAndUser_IdForUpdate(MODEL_ID, USER_ID)).willReturn(Optional.of(model));
        given(datasetRepository.findById(DATASET_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> auditStartService.start(USER_ID, request()))
                .isInstanceOf(DatasetNotFoundException.class);
    }
}
