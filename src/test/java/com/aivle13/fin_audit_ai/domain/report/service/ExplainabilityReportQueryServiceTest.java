package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.report.dto.ExplainabilityReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.exception.report.ReportNotFoundException;
import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExplainabilityReportQueryServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 21L;
    private static final Long REPORT_ID = 31L;
    private static final String S3_KEY =
            "explainability-reports/21/run-123/report.html";

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ReportEntity report;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private ExplainabilityReportQueryService service;

    @Test
    void returnsLatestExplainabilityReportByFormat() {
        LocalDateTime generatedAt =
                LocalDateTime.of(2026, 7, 29, 10, 0);

        given(reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        AUDIT_ID,
                        USER_ID,
                        ReportType.XAI_REPORT,
                        ReportFormat.PDF
                ))
                .willReturn(Optional.of(report));

        given(report.getId()).willReturn(REPORT_ID);
        given(report.getAudit()).willReturn(audit);
        given(audit.getId()).willReturn(AUDIT_ID);
        given(report.getReportType())
                .willReturn(ReportType.XAI_REPORT);
        given(report.getFormat()).willReturn(ReportFormat.PDF);
        given(report.getStatus())
                .willReturn(ReportStatus.COMPLETED);
        given(report.getVersion()).willReturn(1);
        given(report.getCreatedAt()).willReturn(generatedAt);

        ExplainabilityReportMetadataResponse response =
                service.getLatest(
                        USER_ID,
                        AUDIT_ID,
                        ReportFormat.PDF
                );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.auditId()).isEqualTo(AUDIT_ID);
        assertThat(response.reportType())
                .isEqualTo(ReportType.XAI_REPORT);
        assertThat(response.format())
                .isEqualTo(ReportFormat.PDF);
        assertThat(response.status())
                .isEqualTo(ReportStatus.COMPLETED);
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.generatedAt())
                .isEqualTo(generatedAt);
    }

    @Test
    void downloadsOwnedExplainabilityReport() {
        byte[] content = "<html>report</html>".getBytes();

        given(reportRepository
                .findByIdAndAudit_IdAndAudit_User_IdAndReportType(
                        REPORT_ID,
                        AUDIT_ID,
                        USER_ID,
                        ReportType.XAI_REPORT
                ))
                .willReturn(Optional.of(report));

        given(report.getFilePath()).willReturn(S3_KEY);
        given(report.getFormat()).willReturn(ReportFormat.HTML);

        given(fileStorageService.download(S3_KEY))
                .willReturn(
                        new DownloadedFile(
                                new ByteArrayInputStream(content),
                                "text/html",
                                content.length
                        )
                );

        ExplainabilityReportQueryService.ReportDownload download =
                service.download(
                        USER_ID,
                        AUDIT_ID,
                        REPORT_ID
                );

        assertThat(download.filename())
                .isEqualTo(
                        "explainability-report-21.html"
                );
        assertThat(download.contentType())
                .isEqualTo("text/html; charset=UTF-8");
        assertThat(download.size()).isEqualTo(content.length);
        assertThat(download.content()).isNotNull();

        verify(fileStorageService).download(S3_KEY);
    }

    @Test
    void rejectsReportNotOwnedByUser() {
        given(reportRepository
                .findByIdAndAudit_IdAndAudit_User_IdAndReportType(
                        REPORT_ID,
                        AUDIT_ID,
                        USER_ID,
                        ReportType.XAI_REPORT
                ))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.download(
                        USER_ID,
                        AUDIT_ID,
                        REPORT_ID
                )
        ).isInstanceOf(ReportNotFoundException.class);

        verify(fileStorageService, never())
                .download(S3_KEY);
    }

    @Test
    void downloadsPdfExplainabilityReport() {
        String pdfS3Key =
                "explainability-reports/21/run-123/report.pdf";
        byte[] content = "%PDF-test".getBytes();

        given(reportRepository
                .findByIdAndAudit_IdAndAudit_User_IdAndReportType(
                        REPORT_ID,
                        AUDIT_ID,
                        USER_ID,
                        ReportType.XAI_REPORT
                ))
                .willReturn(Optional.of(report));

        given(report.getFilePath()).willReturn(pdfS3Key);
        given(report.getFormat()).willReturn(ReportFormat.PDF);

        given(fileStorageService.download(pdfS3Key))
                .willReturn(
                        new DownloadedFile(
                                new ByteArrayInputStream(content),
                                "application/octet-stream",
                                content.length
                        )
                );

        ExplainabilityReportQueryService.ReportDownload download =
                service.download(
                        USER_ID,
                        AUDIT_ID,
                        REPORT_ID
                );

        assertThat(download.filename())
                .isEqualTo("explainability-report-21.pdf");
        assertThat(download.contentType())
                .isEqualTo("application/pdf");
        assertThat(download.size()).isEqualTo(content.length);
        assertThat(download.content()).isNotNull();

        verify(fileStorageService).download(pdfS3Key);
    }

    @Test
    void downloadsWordExplainabilityReport() {
        String wordS3Key =
                "explainability-reports/21/run-123/report.docx";
        byte[] content = "PK-docx-test".getBytes();

        given(reportRepository
                .findByIdAndAudit_IdAndAudit_User_IdAndReportType(
                        REPORT_ID,
                        AUDIT_ID,
                        USER_ID,
                        ReportType.XAI_REPORT
                ))
                .willReturn(Optional.of(report));

        given(report.getFilePath()).willReturn(wordS3Key);
        given(report.getFormat()).willReturn(ReportFormat.WORD);

        given(fileStorageService.download(wordS3Key))
                .willReturn(
                        new DownloadedFile(
                                new ByteArrayInputStream(content),
                                "application/octet-stream",
                                content.length
                        )
                );

        ExplainabilityReportQueryService.ReportDownload download =
                service.download(
                        USER_ID,
                        AUDIT_ID,
                        REPORT_ID
                );

        assertThat(download.filename())
                .isEqualTo("explainability-report-21.docx");
        assertThat(download.contentType())
                .isEqualTo(
                        "application/vnd.openxmlformats-officedocument."
                                + "wordprocessingml.document"
                );
        assertThat(download.size()).isEqualTo(content.length);
        assertThat(download.content()).isNotNull();

        verify(fileStorageService).download(wordS3Key);
    }
}
