package com.aivle13.fin_audit_ai.domain.report.controller;

import com.aivle13.fin_audit_ai.domain.report.dto.HighImpactReportGenerationResponse;
import com.aivle13.fin_audit_ai.domain.report.dto.HighImpactReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.service.HighImpactReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.HighImpactReportQueryService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HighImpactReportControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 8L;
    private static final Long REPORT_ID = 50L;

    @Mock
    private HighImpactReportGenerationService generationService;

    @Mock
    private HighImpactReportQueryService queryService;

    @InjectMocks
    private HighImpactReportController controller;

    @Test
    void generatesHighImpactReport() {
        given(generationService.generateAndSave(
                USER_ID,
                AUDIT_ID
        )).willReturn(REPORT_ID);

        ResponseEntity<HighImpactReportGenerationResponse> response =
                controller.generate(USER_ID, AUDIT_ID);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().reportId())
                .isEqualTo(REPORT_ID);

        verify(generationService).generateAndSave(
                USER_ID,
                AUDIT_ID
        );
    }

    @Test
    void returnsLatestHighImpactReport() {
        HighImpactReportMetadataResponse metadata =
                new HighImpactReportMetadataResponse(
                        REPORT_ID,
                        AUDIT_ID,
                        ReportType.HIGH_IMPACT_REPORT,
                        ReportFormat.PDF,
                        ReportStatus.COMPLETED,
                        1,
                        LocalDateTime.of(2026, 7, 31, 10, 0)
                );

        given(queryService.getLatest(
                USER_ID,
                AUDIT_ID,
                ReportFormat.PDF
        )).willReturn(metadata);

        ResponseEntity<HighImpactReportMetadataResponse> response =
                controller.getLatest(
                        USER_ID,
                        AUDIT_ID,
                        ReportFormat.PDF
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(metadata);

        verify(queryService).getLatest(
                USER_ID,
                AUDIT_ID,
                ReportFormat.PDF
        );
    }

    @Test
    void downloadsPdfHighImpactReport() {
        byte[] content = "%PDF-test".getBytes();

        given(queryService.download(
                USER_ID,
                AUDIT_ID,
                REPORT_ID
        )).willReturn(
                new HighImpactReportQueryService.ReportDownload(
                        "high-impact-assessment-8.pdf",
                        "application/pdf",
                        content.length,
                        new ByteArrayInputStream(content)
                )
        );

        ResponseEntity<InputStreamResource> response =
                controller.download(
                        USER_ID,
                        AUDIT_ID,
                        REPORT_ID
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .hasToString("application/pdf");
        assertThat(response.getHeaders().getContentLength())
                .isEqualTo(content.length);
        assertThat(
                response.getHeaders().getFirst(
                        HttpHeaders.CONTENT_DISPOSITION
                )
        ).contains("high-impact-assessment-8.pdf");
        assertThat(response.getBody()).isNotNull();

        verify(queryService).download(
                USER_ID,
                AUDIT_ID,
                REPORT_ID
        );
    }

    @Test
    void rejectsUnauthenticatedGenerationRequest() {
        assertThatThrownBy(() ->
                controller.generate(null, AUDIT_ID)
        ).isInstanceOf(UnauthorizedException.class);

        verify(generationService, never())
                .generateAndSave(USER_ID, AUDIT_ID);
    }

    @Test
    void rejectsUnauthenticatedQueryRequest() {
        assertThatThrownBy(() ->
                controller.getLatest(
                        null,
                        AUDIT_ID,
                        ReportFormat.PDF
                )
        ).isInstanceOf(UnauthorizedException.class);

        verify(queryService, never())
                .getLatest(
                        USER_ID,
                        AUDIT_ID,
                        ReportFormat.PDF
                );
    }

    @Test
    void rejectsUnauthenticatedDownloadRequest() {
        assertThatThrownBy(() ->
                controller.download(
                        null,
                        AUDIT_ID,
                        REPORT_ID
                )
        ).isInstanceOf(UnauthorizedException.class);

        verify(queryService, never())
                .download(USER_ID, AUDIT_ID, REPORT_ID);
    }
}
