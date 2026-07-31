package com.aivle13.fin_audit_ai.domain.audit.service.explainability;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.explainability.ExplainabilityResultRequest;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.global.ai.client.ShapAnalysisClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ShapAnalysisRequest;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShapAnalysisServiceTest {

    private static final Long AUDIT_ID = 21L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private ShapAnalysisClient shapAnalysisClient;

    @Mock
    private ExplainabilityService explainabilityService;

    @Mock
    private AuditEntity audit;

    @Mock
    private AiModelEntity model;

    @Mock
    private DatasetEntity dataset;

    @Mock
    private ExplainabilityResultRequest response;

    @InjectMocks
    private ShapAnalysisService shapAnalysisService;

    @Test
    void analyzesAndSavesShapResult() {
        given(auditRepository.findByIdWithModelAndDataset(AUDIT_ID))
                .willReturn(Optional.of(audit));

        given(audit.getId()).willReturn(AUDIT_ID);
        given(audit.getModel()).willReturn(model);
        given(audit.getDataset()).willReturn(dataset);
        given(audit.getSensitiveFeatures())
                .willReturn(
                        "CODE_GENDER, AGE_GROUP, CODE_GENDER"
                );

        given(model.getArtifactPath())
                .willReturn("models/model.json");

        given(dataset.getDatasetFileKey())
                .willReturn("datasets/audit.csv");

        given(shapAnalysisClient.analyze(
                org.mockito.ArgumentMatchers.any(
                        ShapAnalysisRequest.class
                )
        )).willReturn(response);

        shapAnalysisService.analyzeAndSave(AUDIT_ID);

        ArgumentCaptor<ShapAnalysisRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        ShapAnalysisRequest.class
                );

        verify(shapAnalysisClient)
                .analyze(requestCaptor.capture());

        ShapAnalysisRequest request =
                requestCaptor.getValue();

        assertThat(request.auditId()).isEqualTo(AUDIT_ID);
        assertThat(request.modelS3Key())
                .isEqualTo("models/model.json");
        assertThat(request.auditDatasetS3Key())
                .isEqualTo("datasets/audit.csv");
        assertThat(request.targetColumn())
                .isEqualTo("TARGET");
        assertThat(request.sensitiveFeatures())
                .containsExactly(
                        "CODE_GENDER",
                        "AGE_GROUP"
                );
        assertThat(request.includeReport()).isTrue();
        assertThat(request.reportTopN()).isEqualTo(5);

        verify(explainabilityService)
                .saveExplainabilityResult(
                        AUDIT_ID,
                        response
                );
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdWithModelAndDataset(AUDIT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                shapAnalysisService.analyzeAndSave(AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);

        verify(shapAnalysisClient, never())
                .analyze(
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void throwsWhenDatasetS3KeyIsMissing() {
        given(auditRepository.findByIdWithModelAndDataset(AUDIT_ID))
                .willReturn(Optional.of(audit));

        given(audit.getModel()).willReturn(model);
        given(audit.getDataset()).willReturn(dataset);

        given(model.getArtifactPath())
                .willReturn("models/model.json");

        given(dataset.getDatasetFileKey())
                .willReturn(null);

        assertThatThrownBy(() ->
                shapAnalysisService.analyzeAndSave(AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(shapAnalysisClient, never())
                .analyze(
                        org.mockito.ArgumentMatchers.any()
                );
    }
}