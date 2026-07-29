package com.aivle13.fin_audit_ai.domain.report.controller;

import com.aivle13.fin_audit_ai.domain.report.dto.ExplainabilityReportGenerationResponse;
import com.aivle13.fin_audit_ai.domain.report.dto.ExplainabilityReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.service.ExplainabilityReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.ExplainabilityReportQueryService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ExplainabilityReportControllerTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 21L;
    private static final Long REPORT_ID = 31L;

    @Mock
    private ExplainabilityReportGenerationService generationService;

    @Mock
    private ExplainabilityReportQueryService queryService;

    @InjectMocks
    private ExplainabilityReportController controller;

    @Test
    void returnsCreatedReportId() {
        given(generationService.generateAndSave(
                USER_ID,
                AUDIT_ID
        )).willReturn(REPORT_ID);

        ResponseEntity<ExplainabilityReportGenerationResponse> response =
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
    void returnsLatestReportMetadata() {
        ExplainabilityReportMetadataResponse expected =
                new ExplainabilityReportMetadataResponse(
                        REPORT_ID,
                        AUDIT_ID,
                        ReportType.XAI_REPORT,
                        ReportFormat.HTML,
                        ReportStatus.COMPLETED,
                        1,
                        LocalDateTime.of(
                                2026,
                                7,
                                29,
                                10,
                                0
                        )
                );

        given(queryService.getLatest(USER_ID, AUDIT_ID))
                .willReturn(expected);

        ResponseEntity<ExplainabilityReportMetadataResponse> response =
                controller.getLatest(USER_ID, AUDIT_ID);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);

        verify(queryService).getLatest(USER_ID, AUDIT_ID);
    }

    @Test
    void downloadsHtmlReport() {
        byte[] content = "<html>report</html>".getBytes();

        given(queryService.download(
                USER_ID,
                AUDIT_ID,
                REPORT_ID
        )).willReturn(
                new ExplainabilityReportQueryService.ReportDownload(
                        "explainability-report-21.html",
                        "text/html",
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
                .isEqualTo(MediaType.TEXT_HTML);
        assertThat(response.getHeaders().getContentLength())
                .isEqualTo(content.length);
        assertThat(
                response.getHeaders().getFirst(
                        HttpHeaders.CONTENT_DISPOSITION
                )
        ).contains("explainability-report-21.html");
        assertThat(response.getBody()).isNotNull();

        verify(queryService).download(
                USER_ID,
                AUDIT_ID,
                REPORT_ID
        );
    }

    @Test
    void throwsWhenUserIsNotAuthenticated() {
        assertThatThrownBy(() ->
                controller.generate(null, AUDIT_ID)
        ).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(
                generationService,
                queryService
        );
    }
}
