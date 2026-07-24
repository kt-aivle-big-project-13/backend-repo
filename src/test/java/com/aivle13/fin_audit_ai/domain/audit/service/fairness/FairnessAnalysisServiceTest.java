package com.aivle13.fin_audit_ai.domain.audit.service.fairness;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.fairness.FairnessRunRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessRunResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.global.ai.client.FairnessAnalysisClient;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FairnessAnalysisServiceTest {

    private static final Long AUDIT_ID = 21L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private FairnessAnalysisClient fairnessAnalysisClient;

    @Mock
    private FairnessResultService fairnessResultService;

    @Mock
    private AuditEntity audit;

    @Mock
    private AiModelEntity model;

    @Mock
    private DatasetEntity dataset;

    @Mock
    private DatasetEntity validationDataset;

    @Mock
    private FairnessRunResponse response;

    @InjectMocks
    private FairnessAnalysisService fairnessAnalysisService;

    @Test
    void analyzesAndSavesFairnessResultWithValidationDataset() {
        given(auditRepository.findByIdWithModelAndDataset(AUDIT_ID))
                .willReturn(Optional.of(audit));

        given(audit.getId()).willReturn(AUDIT_ID);
        given(audit.getModel()).willReturn(model);
        given(audit.getDataset()).willReturn(dataset);
        given(audit.getSensitiveFeatures())
                .willReturn("CODE_GENDER, AGE_GROUP, CODE_GENDER");
        given(audit.getAuditName()).willReturn("테스트 감사");
        given(audit.getTargetApprovalRate())
                .willReturn(new BigDecimal("0.9"));
        given(audit.getManualThreshold()).willReturn(null);

        given(model.getArtifactPath()).willReturn("models/model.json");
        given(dataset.getDatasetFileKey()).willReturn("datasets/audit.csv");

        given(audit.getValidationDataset()).willReturn(validationDataset);
        given(validationDataset.getDatasetFileKey())
                .willReturn("datasets/valid.csv");

        given(fairnessAnalysisClient.analyze(any(FairnessRunRequest.class)))
                .willReturn(response);

        fairnessAnalysisService.analyzeAndSave(AUDIT_ID);

        ArgumentCaptor<FairnessRunRequest> requestCaptor =
                ArgumentCaptor.forClass(FairnessRunRequest.class);

        verify(fairnessAnalysisClient).analyze(requestCaptor.capture());

        FairnessRunRequest request = requestCaptor.getValue();

        assertThat(request.auditId()).isEqualTo(AUDIT_ID);
        assertThat(request.modelFileKey()).isEqualTo("models/model.json");
        assertThat(request.auditDatasetFileKey()).isEqualTo("datasets/audit.csv");
        assertThat(request.validationDatasetFileKey()).isEqualTo("datasets/valid.csv");
        assertThat(request.auditName()).isEqualTo("테스트 감사");
        assertThat(request.targetApprovalRate())
                .isEqualByComparingTo("0.9");

        verify(fairnessResultService)
                .saveFairnessResult(AUDIT_ID, response);
    }

    @Test
    void passesNullValidationDatasetKeyWhenNoneExists() {
        given(auditRepository.findByIdWithModelAndDataset(AUDIT_ID))
                .willReturn(Optional.of(audit));

        given(audit.getId()).willReturn(AUDIT_ID);
        given(audit.getModel()).willReturn(model);
        given(audit.getDataset()).willReturn(dataset);
        given(audit.getSensitiveFeatures()).willReturn("CODE_GENDER");
        given(audit.getAuditName()).willReturn("테스트 감사");

        given(model.getArtifactPath()).willReturn("models/model.json");
        given(dataset.getDatasetFileKey()).willReturn("datasets/audit.csv");

        given(audit.getValidationDataset()).willReturn(null);

        given(fairnessAnalysisClient.analyze(any(FairnessRunRequest.class)))
                .willReturn(response);

        fairnessAnalysisService.analyzeAndSave(AUDIT_ID);

        ArgumentCaptor<FairnessRunRequest> requestCaptor =
                ArgumentCaptor.forClass(FairnessRunRequest.class);

        verify(fairnessAnalysisClient).analyze(requestCaptor.capture());

        assertThat(requestCaptor.getValue().validationDatasetFileKey()).isNull();
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdWithModelAndDataset(AUDIT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                fairnessAnalysisService.analyzeAndSave(AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);

        verify(fairnessAnalysisClient, never()).analyze(any());
    }

    @Test
    void throwsWhenDatasetS3KeyIsMissing() {
        given(auditRepository.findByIdWithModelAndDataset(AUDIT_ID))
                .willReturn(Optional.of(audit));

        given(audit.getModel()).willReturn(model);
        given(audit.getDataset()).willReturn(dataset);

        given(model.getArtifactPath()).willReturn("models/model.json");
        given(dataset.getDatasetFileKey()).willReturn(null);

        assertThatThrownBy(() ->
                fairnessAnalysisService.analyzeAndSave(AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(fairnessAnalysisClient, never()).analyze(any());
    }

    @Test
    void throwsWhenSensitiveFeaturesAreBlank() {
        given(auditRepository.findByIdWithModelAndDataset(AUDIT_ID))
                .willReturn(Optional.of(audit));

        given(audit.getModel()).willReturn(model);
        given(audit.getDataset()).willReturn(dataset);
        given(audit.getSensitiveFeatures()).willReturn("  ,  ");

        given(model.getArtifactPath()).willReturn("models/model.json");
        given(dataset.getDatasetFileKey()).willReturn("datasets/audit.csv");

        assertThatThrownBy(() ->
                fairnessAnalysisService.analyzeAndSave(AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(fairnessAnalysisClient, never()).analyze(any());
    }
}
