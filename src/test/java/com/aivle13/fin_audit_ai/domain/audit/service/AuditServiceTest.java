package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void validationDatasetPath를_포함해_AuditEntity를_저장한다() {
        AiModelEntity aiModel = AiModelEntity.create(null, "my-model", ModelType.XGBOOST, "models/model.json");
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(auditRepository.save(any(AuditEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        auditService.create(USER_ID, aiModel, "datasets/audit-key.csv", "datasets/validation-key.csv", "age,gender");

        ArgumentCaptor<AuditEntity> captor = ArgumentCaptor.forClass(AuditEntity.class);
        verify(auditRepository).save(captor.capture());
        AuditEntity saved = captor.getValue();
        assertThat(saved.getDatasetPath()).isEqualTo("datasets/audit-key.csv");
        assertThat(saved.getValidationDatasetPath()).isEqualTo("datasets/validation-key.csv");
        assertThat(saved.getSensitiveFeatures()).isEqualTo("age,gender");
    }

    @Test
    void validationDatasetPath가_없으면_null로_저장한다() {
        AiModelEntity aiModel = AiModelEntity.create(null, "my-model", ModelType.XGBOOST, "models/model.json");
        given(userRepository.getReferenceById(USER_ID)).willReturn(user);
        given(auditRepository.save(any(AuditEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        auditService.create(USER_ID, aiModel, "datasets/audit-key.csv", null, "age,gender");

        ArgumentCaptor<AuditEntity> captor = ArgumentCaptor.forClass(AuditEntity.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getValidationDatasetPath()).isNull();
    }
}
