package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditSummaryResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserEntity user;

    @InjectMocks
    private AuditService auditService;

    private static final Long USER_ID = 1L;

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
        assertThat(summary.status()).isEqualTo(AuditStatus.COMPLIANT);
        assertThat(summary.currentStep()).isEqualTo(4);
        assertThat(summary.completedAt()).isNotNull();
    }
}
