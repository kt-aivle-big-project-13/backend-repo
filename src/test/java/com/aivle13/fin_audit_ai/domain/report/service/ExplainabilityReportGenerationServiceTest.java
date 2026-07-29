package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.ExplainabilityReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.ExplainabilityReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ExplainabilityReportResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExplainabilityReportGenerationServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 21L;
    private static final Long REPORT_ID = 31L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private ExplainabilityReportClient reportClient;

    @Mock
    private ReportPersistenceService reportPersistenceService;

    @Mock
    private AuditEntity audit;

    @Mock
    private AiModelEntity model;

    @Mock
    private DatasetEntity dataset;

    @InjectMocks
    private ExplainabilityReportGenerationService service;

    @Test
    void generatesAndSavesExplainabilityReport() {
        givenAudit();

        ExplainabilityReportResponse response =
                new ExplainabilityReportResponse(
                        AUDIT_ID,
                        "explainability-reports/21/run-123/report.html",
                        "html",
                        "WARNING",
                        "2026-07-29T10:00:00Z"
                );

        given(reportClient.generate(
                any(ExplainabilityReportRequest.class)
        )).willReturn(response);

        given(reportPersistenceService.save(
                AUDIT_ID,
                ReportType.XAI_REPORT,
                ReportFormat.HTML,
                response.reportS3Key()
        )).willReturn(REPORT_ID);

        Long result = service.generateAndSave(USER_ID, AUDIT_ID);

        assertThat(result).isEqualTo(REPORT_ID);

        ArgumentCaptor<ExplainabilityReportRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        ExplainabilityReportRequest.class
                );

        verify(reportClient)
                .generate(requestCaptor.capture());

        ExplainabilityReportRequest request =
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
        assertThat(request.reportTopN()).isEqualTo(20);

        verify(reportPersistenceService).save(
                AUDIT_ID,
                ReportType.XAI_REPORT,
                ReportFormat.HTML,
                response.reportS3Key()
        );
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

        ExplainabilityReportResponse invalidResponse =
                new ExplainabilityReportResponse(
                        AUDIT_ID,
                        "",
                        "html",
                        "WARNING",
                        "2026-07-29T10:00:00Z"
                );

        given(reportClient.generate(
                any(ExplainabilityReportRequest.class)
        )).willReturn(invalidResponse);

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportPersistenceService, never())
                .save(any(), any(), any(), any());
    }

    private void givenAudit() {
        given(auditRepository.findByIdAndUser_IdWithModelAndDataset(
                AUDIT_ID,
                USER_ID
        ))
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
    }
}
