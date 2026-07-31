package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.BiasReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.BiasReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.BiasReportResponse;
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
class BiasReportGenerationServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 21L;
    private static final Long REPORT_ID = 31L;

    private static final BigDecimal TARGET_APPROVAL_RATE =
            new BigDecimal("0.8500");

    private static final BigDecimal MANUAL_THRESHOLD =
            new BigDecimal("0.3200");

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private BiasReportClient reportClient;

    @Mock
    private ReportPersistenceService reportPersistenceService;

    @Mock
    private AuditEntity audit;

    @Mock
    private AiModelEntity model;

    @Mock
    private DatasetEntity dataset;

    @Mock
    private DatasetEntity validationDataset;

    @InjectMocks
    private BiasReportGenerationService service;

    @Test
    void generatesAndSavesBiasReport() {
        givenAudit();
        givenValidationDatasetThreshold();

        BiasReportResponse response =
                new BiasReportResponse(
                        AUDIT_ID,
                        "bias-reports/21/run-123/report.html",
                        "html",
                        "2026-07-30T10:00:00Z"
                );

        given(reportClient.generate(
                any(BiasReportRequest.class)
        )).willReturn(response);

        given(reportPersistenceService.save(
                AUDIT_ID,
                ReportType.BIAS_REPORT,
                ReportFormat.HTML,
                response.reportS3Key()
        )).willReturn(REPORT_ID);

        Long result = service.generateAndSave(USER_ID, AUDIT_ID);

        assertThat(result).isEqualTo(REPORT_ID);

        BiasReportRequest request = capturedRequest();

        assertThat(request.auditId()).isEqualTo(AUDIT_ID);
        assertThat(request.modelS3Key())
                .isEqualTo("models/model.json");
        assertThat(request.auditDatasetS3Key())
                .isEqualTo("datasets/audit.csv");
        assertThat(request.auditName())
                .isEqualTo("테스트 감사");
        assertThat(request.sensitiveFeatures())
                .containsExactly(
                        "CODE_GENDER",
                        "AGE_GROUP"
                );

        verify(reportPersistenceService).save(
                AUDIT_ID,
                ReportType.BIAS_REPORT,
                ReportFormat.HTML,
                response.reportS3Key()
        );
    }

    @Test
    void sendsValidationDatasetAndTargetApprovalRate() {
        givenAudit();
        givenValidationDatasetThreshold();
        givenSuccessfulReportGeneration();

        service.generateAndSave(USER_ID, AUDIT_ID);

        BiasReportRequest request = capturedRequest();

        assertThat(request.validationDatasetS3Key())
                .isEqualTo("datasets/validation.csv");
        assertThat(request.targetApprovalRate())
                .isEqualByComparingTo(TARGET_APPROVAL_RATE);
        assertThat(request.manualThreshold()).isNull();
    }

    @Test
    void sendsManualThresholdWithoutValidationDataset() {
        givenAudit();
        givenManualThreshold();
        givenSuccessfulReportGeneration();

        service.generateAndSave(USER_ID, AUDIT_ID);

        BiasReportRequest request = capturedRequest();

        assertThat(request.validationDatasetS3Key()).isNull();
        assertThat(request.targetApprovalRate()).isNull();
        assertThat(request.manualThreshold())
                .isEqualByComparingTo(MANUAL_THRESHOLD);
    }

    @Test
    void rejectsValidationDatasetWithoutFileKey() {
        givenAuditLookup();

        given(audit.getValidationDataset())
                .willReturn(validationDataset);
        given(validationDataset.getDatasetFileKey())
                .willReturn("");

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportClient, never())
                .generate(any());
        verify(reportPersistenceService, never())
                .save(any(), any(), any(), any());
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdAndUser_IdWithModelAndDataset(
                AUDIT_ID,
                USER_ID
        ))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);

        verify(reportClient, never())
                .generate(any());
        verify(reportPersistenceService, never())
                .save(any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidAiServerResponse() {
        givenAudit();
        givenValidationDatasetThreshold();

        BiasReportResponse invalidResponse =
                new BiasReportResponse(
                        AUDIT_ID,
                        "",
                        "html",
                        "2026-07-30T10:00:00Z"
                );

        given(reportClient.generate(
                any(BiasReportRequest.class)
        )).willReturn(invalidResponse);

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportPersistenceService, never())
                .save(any(), any(), any(), any());
    }

    private BiasReportRequest capturedRequest() {
        ArgumentCaptor<BiasReportRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        BiasReportRequest.class
                );

        verify(reportClient)
                .generate(requestCaptor.capture());

        return requestCaptor.getValue();
    }

    private void givenSuccessfulReportGeneration() {
        BiasReportResponse response =
                new BiasReportResponse(
                        AUDIT_ID,
                        "bias-reports/21/run-123/report.html",
                        "html",
                        "2026-07-30T10:00:00Z"
                );

        given(reportClient.generate(
                any(BiasReportRequest.class)
        )).willReturn(response);
    }

    // 모델·감사 데이터셋 키까지만 필요한 셋업. 그 뒤 단계에서 실패하는 테스트가 쓴다.
    private void givenAuditLookup() {
        given(auditRepository.findByIdAndUser_IdWithModelAndDataset(
                AUDIT_ID,
                USER_ID
        ))
                .willReturn(Optional.of(audit));

        given(audit.getModel()).willReturn(model);
        given(audit.getDataset()).willReturn(dataset);

        given(model.getArtifactPath())
                .willReturn("models/model.json");

        given(dataset.getDatasetFileKey())
                .willReturn("datasets/audit.csv");
    }

    private void givenAudit() {
        givenAuditLookup();

        given(audit.getId()).willReturn(AUDIT_ID);
        given(audit.getAuditName())
                .willReturn("테스트 감사");
        given(audit.getSensitiveFeatures())
                .willReturn(
                        "CODE_GENDER, AGE_GROUP, CODE_GENDER"
                );
    }

    // 검증 데이터셋으로 목표 승인율에 맞춰 임계값을 산출하는 감사
    private void givenValidationDatasetThreshold() {
        given(audit.getValidationDataset())
                .willReturn(validationDataset);
        given(validationDataset.getDatasetFileKey())
                .willReturn("datasets/validation.csv");
        given(audit.getTargetApprovalRate())
                .willReturn(TARGET_APPROVAL_RATE);
        given(audit.getManualThreshold())
                .willReturn(null);
    }

    // 사용자가 임계값을 직접 지정해 검증 데이터셋이 없는 감사
    private void givenManualThreshold() {
        given(audit.getValidationDataset())
                .willReturn(null);
        given(audit.getTargetApprovalRate())
                .willReturn(null);
        given(audit.getManualThreshold())
                .willReturn(MANUAL_THRESHOLD);
    }
}
