package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.report.service.bias.BiasReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportNarrativeRecorder;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPersistenceService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.ai.client.report.BiasReportClient;
import com.aivle13.fin_audit_ai.global.ai.dto.report.request.BiasReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.BiasReportResponse;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ReportNarrativeResponse;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import java.math.BigDecimal;
import java.util.Map;
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
    private static final Long PDF_REPORT_ID = 32L;
    private static final Long WORD_REPORT_ID = 33L;

    private static final String HTML_S3_KEY =
            "bias-reports/21/run-123/report.html";
    private static final String PDF_S3_KEY =
            "bias-reports/21/run-123/report.pdf";
    private static final String WORD_S3_KEY =
            "bias-reports/21/run-123/report.docx";

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
    private ReportNarrativeRecorder narrativeRecorder;

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
    void savesReportNarrativesForChatbot() {
        givenAudit();
        givenValidationDatasetThreshold();

        List<ReportNarrativeResponse> narratives = List.of(
                new ReportNarrativeResponse(
                        "metric_results",
                        "5. 공정성 지표 결과",
                        "AGE_GROUP 의 Equal Opportunity Difference 는 0.1123 으로 확인됨"
                )
        );

        given(reportClient.generate(any(BiasReportRequest.class)))
                .willReturn(new BiasReportResponse(
                        AUDIT_ID,
                        HTML_S3_KEY,
                        PDF_S3_KEY,
                        WORD_S3_KEY,
                        "html",
                        "2026-07-30T10:00:00Z",
                        narratives
                ));

        given(reportPersistenceService.saveAll(any(), any(), any()))
                .willReturn(Map.of(ReportFormat.HTML, REPORT_ID));

        service.generateAndSave(USER_ID, AUDIT_ID);

        verify(narrativeRecorder).record(
                AUDIT_ID,
                ReportType.BIAS_REPORT,
                narratives
        );
    }

    @Test
    void generatesAndSavesBiasReport() {
        givenAudit();
        givenValidationDatasetThreshold();
        givenSuccessfulReportGeneration();

        Long result = service.generateAndSave(USER_ID, AUDIT_ID);

        // 생성 응답은 기존 계약대로 HTML 리포트 ID를 반환한다.
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
    }

    @Test
    void savesHtmlPdfAndWordInOneTransaction() {
        givenAudit();
        givenValidationDatasetThreshold();
        givenSuccessfulReportGeneration();

        service.generateAndSave(USER_ID, AUDIT_ID);

        verify(reportPersistenceService).saveAll(
                AUDIT_ID,
                ReportType.BIAS_REPORT,
                Map.of(
                        ReportFormat.HTML, HTML_S3_KEY,
                        ReportFormat.PDF, PDF_S3_KEY,
                        ReportFormat.WORD, WORD_S3_KEY
                )
        );
    }

    @Test
    void rejectsResponseWithoutPdfKey() {
        givenAudit();
        givenValidationDatasetThreshold();

        BiasReportResponse missingPdf =
                new BiasReportResponse(
                        AUDIT_ID,
                        HTML_S3_KEY,
                        "",
                        WORD_S3_KEY,
                        "html",
                        "2026-07-30T10:00:00Z",
                        List.of()
                );

        given(reportClient.generate(
                any(BiasReportRequest.class)
        )).willReturn(missingPdf);

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportPersistenceService, never())
                .saveAll(any(), any(), any());
    }

    @Test
    void rejectsResponseWithoutWordKey() {
        givenAudit();
        givenValidationDatasetThreshold();

        BiasReportResponse missingWord =
                new BiasReportResponse(
                        AUDIT_ID,
                        HTML_S3_KEY,
                        PDF_S3_KEY,
                        "",
                        "html",
                        "2026-07-30T10:00:00Z",
                        List.of()
                );

        given(reportClient.generate(
                any(BiasReportRequest.class)
        )).willReturn(missingWord);

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportPersistenceService, never())
                .saveAll(any(), any(), any());
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
                .saveAll(any(), any(), any());
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
                .saveAll(any(), any(), any());
    }

    @Test
    void rejectsInvalidAiServerResponse() {
        givenAudit();
        givenValidationDatasetThreshold();

        BiasReportResponse invalidResponse =
                new BiasReportResponse(
                        AUDIT_ID,
                        "",
                        PDF_S3_KEY,
                        WORD_S3_KEY,
                        "html",
                        "2026-07-30T10:00:00Z",
                        List.of()
                );

        given(reportClient.generate(
                any(BiasReportRequest.class)
        )).willReturn(invalidResponse);

        assertThatThrownBy(() ->
                service.generateAndSave(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verify(reportPersistenceService, never())
                .saveAll(any(), any(), any());
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
                        HTML_S3_KEY,
                        PDF_S3_KEY,
                        WORD_S3_KEY,
                        "html",
                        "2026-07-30T10:00:00Z",
                        List.of()
                );

        given(reportClient.generate(
                any(BiasReportRequest.class)
        )).willReturn(response);

        given(reportPersistenceService.saveAll(
                AUDIT_ID,
                ReportType.BIAS_REPORT,
                Map.of(
                        ReportFormat.HTML, HTML_S3_KEY,
                        ReportFormat.PDF, PDF_S3_KEY,
                        ReportFormat.WORD, WORD_S3_KEY
                )
        )).willReturn(
                Map.of(
                        ReportFormat.HTML, REPORT_ID,
                        ReportFormat.PDF, PDF_REPORT_ID
                )
        );
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
