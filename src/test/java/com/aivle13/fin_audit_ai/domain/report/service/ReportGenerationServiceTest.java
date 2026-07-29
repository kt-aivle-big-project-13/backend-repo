package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.document.GeneratedReportFile;
import com.aivle13.fin_audit_ai.domain.report.document.ReportDocumentGenerator;
import com.aivle13.fin_audit_ai.domain.report.dto.ReportGenerationContext;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    private static final Long AUDIT_ID = 21L;
    private static final Long REPORT_ID = 99L;

    @Mock
    private ReportLlmClient reportLlmClient;

    @Mock
    private ReportGenerationContextLoader contextLoader;

    @Mock
    private ReportDocumentGenerator reportDocumentGenerator;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ReportPersistenceService reportPersistenceService;

    @InjectMocks
    private ReportGenerationService reportGenerationService;

    @Test
    void generatesReportWithoutAnyPendingMappingGate() {
        ReportGenerationContext context = new ReportGenerationContext(
                AUDIT_ID, List.of(), List.of(), List.of(), List.of(), List.of()
        );
        given(contextLoader.load(AUDIT_ID)).willReturn(context);

        given(reportLlmClient.generate(anyString(), anyString()))
                .willReturn("감사 결과를 종합한 보고서 내용입니다.");

        GeneratedReportFile generatedFile = new GeneratedReportFile(
                "content".getBytes(), "report.pdf", "application/pdf"
        );
        given(reportDocumentGenerator.generate(any(), anyString(), any()))
                .willReturn(generatedFile);

        StoredFile storedFile = new StoredFile("s3-key", "report.pdf", "application/pdf", 100L);
        given(fileStorageService.store(any(), anyString(), anyString(), anyString()))
                .willReturn(storedFile);

        given(reportPersistenceService.save(
                AUDIT_ID,
                ReportType.FINAL_AUDIT_REPORT,
                ReportFormat.PDF,
                "s3-key"
        )).willReturn(REPORT_ID);

        Long reportId = reportGenerationService.generate(AUDIT_ID, ReportFormat.PDF);

        assertThat(reportId).isEqualTo(REPORT_ID);
    }
}
