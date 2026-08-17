package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.document.GeneratedReportFile;
import com.aivle13.fin_audit_ai.domain.report.document.ReportDocumentGenerator;
import com.aivle13.fin_audit_ai.domain.report.dto.response.common.GeneratedReportResponse;
import com.aivle13.fin_audit_ai.domain.report.dto.response.common.ReportGenerationContext;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportGenerationContextLoader;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPersistenceService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportGenerationGuard;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    private static final Long AUDIT_ID = 21L;
    private static final Long USER_ID = 2L;
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

    @Mock
    private ReportGenerationGuard generationGuard;

    @InjectMocks
    private ReportGenerationService reportGenerationService;

    /**
     * 중복 방지 가드는 그대로 통과시킨다.
     *
     * <p>이 테스트가 보려는 것은 생성 본문이지 가드가 아니다. 가드 자체는
     * {@link ReportGenerationGuardTest} 에서 따로 본다.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void passThroughGenerationGuard() {
        lenient()
                .when(generationGuard.generateOnce(any(), any(), any(), any()))
                .thenAnswer(invocation ->
                        ((Supplier<Object>) invocation.getArgument(3)).get());
    }

    @Test
    void generatesReportWithoutAnyPendingMappingGate() {
        ReportGenerationContext context = new ReportGenerationContext(
                AUDIT_ID, List.of(), List.of(), List.of(), List.of()
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

        given(reportPersistenceService.saveAll(
                AUDIT_ID,
                ReportType.FINAL_AUDIT_REPORT,
                Map.of(ReportFormat.PDF, "s3-key")
        )).willReturn(Map.of(ReportFormat.PDF, REPORT_ID));

        List<GeneratedReportResponse> responses =
                reportGenerationService.generate(USER_ID, AUDIT_ID, List.of(ReportFormat.PDF));

        assertThat(responses).hasSize(1);

        GeneratedReportResponse response = responses.get(0);

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.reportType())
                .isEqualTo(ReportType.FINAL_AUDIT_REPORT);
        assertThat(response.format())
                .isEqualTo(ReportFormat.PDF);
        assertThat(response.status())
                .isEqualTo(ReportStatus.COMPLETED);
    }
}
