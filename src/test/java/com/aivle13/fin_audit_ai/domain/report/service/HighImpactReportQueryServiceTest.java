package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.report.dto.response.highimpact.HighImpactReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.service.highimpact.HighImpactReportQueryService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HighImpactReportQueryServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long AUDIT_ID = 8L;
    private static final Long REPORT_ID = 50L;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ReportEntity report;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private HighImpactReportQueryService service;

    @Test
    void returnsLatestHighImpactReportByFormat() {
        LocalDateTime generatedAt =
                LocalDateTime.of(2026, 7, 31, 10, 0);

        given(reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        AUDIT_ID,
                        USER_ID,
                        ReportType.HIGH_IMPACT_REPORT,
                        ReportFormat.PDF
                ))
                .willReturn(Optional.of(report));

        given(report.getId()).willReturn(REPORT_ID);
        given(report.getAudit()).willReturn(audit);
        given(audit.getId()).willReturn(AUDIT_ID);
        given(report.getReportType())
                .willReturn(ReportType.HIGH_IMPACT_REPORT);
        given(report.getFormat()).willReturn(ReportFormat.PDF);
        given(report.getStatus())
                .willReturn(ReportStatus.COMPLETED);
        given(report.getVersion()).willReturn(1);
        given(report.getCreatedAt()).willReturn(generatedAt);

        HighImpactReportMetadataResponse response =
                service.getLatest(
                        USER_ID,
                        AUDIT_ID,
                        ReportFormat.PDF
                );

        assertThat(response.reportId()).isEqualTo(REPORT_ID);
        assertThat(response.auditId()).isEqualTo(AUDIT_ID);
        assertThat(response.reportType())
                .isEqualTo(ReportType.HIGH_IMPACT_REPORT);
        assertThat(response.format())
                .isEqualTo(ReportFormat.PDF);
        assertThat(response.status())
                .isEqualTo(ReportStatus.COMPLETED);
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.generatedAt())
                .isEqualTo(generatedAt);
    }

    @Test
    void downloadsPdfHighImpactReport() {
        String pdfS3Key =
                "high-impact-reports/8/50/run-123/report.pdf";
        byte[] content = "%PDF-test".getBytes();

        givenOwnedReport(ReportFormat.PDF, pdfS3Key);

        given(fileStorageService.download(pdfS3Key))
                .willReturn(new DownloadedFile(
                        new ByteArrayInputStream(content),
                        "application/octet-stream",
                        content.length
                ));

        HighImpactReportQueryService.ReportDownload download =
                service.download(USER_ID, AUDIT_ID, REPORT_ID);

        assertThat(download.filename())
                .isEqualTo("high-impact-assessment-8.pdf");
        assertThat(download.contentType())
                .isEqualTo("application/pdf");
        assertThat(download.size()).isEqualTo(content.length);
        assertThat(download.content()).isNotNull();

        verify(fileStorageService).download(pdfS3Key);
    }

    @Test
    void downloadsWordHighImpactReport() {
        String wordS3Key =
                "high-impact-reports/8/50/run-123/report.docx";
        byte[] content = "PK-docx-test".getBytes();

        givenOwnedReport(ReportFormat.WORD, wordS3Key);

        given(fileStorageService.download(wordS3Key))
                .willReturn(new DownloadedFile(
                        new ByteArrayInputStream(content),
                        "application/octet-stream",
                        content.length
                ));

        HighImpactReportQueryService.ReportDownload download =
                service.download(USER_ID, AUDIT_ID, REPORT_ID);

        assertThat(download.filename())
                .isEqualTo("high-impact-assessment-8.docx");
        assertThat(download.contentType())
                .isEqualTo(
                        "application/vnd.openxmlformats-officedocument."
                                + "wordprocessingml.document"
                );
        assertThat(download.size()).isEqualTo(content.length);
        assertThat(download.content()).isNotNull();

        verify(fileStorageService).download(wordS3Key);
    }

    @Test
    void rejectsHtmlFormat() {
        assertThatThrownBy(() ->
                service.getLatest(
                        USER_ID,
                        AUDIT_ID,
                        ReportFormat.HTML
                )
        ).isInstanceOf(ReportNotFoundException.class);

        verify(reportRepository, never())
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        AUDIT_ID,
                        USER_ID,
                        ReportType.HIGH_IMPACT_REPORT,
                        ReportFormat.HTML
                );
    }

    @Test
    void rejectsReportNotOwnedByUser() {
        given(reportRepository
                .findByIdAndAudit_IdAndAudit_User_IdAndReportType(
                        REPORT_ID,
                        AUDIT_ID,
                        USER_ID,
                        ReportType.HIGH_IMPACT_REPORT
                ))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.download(USER_ID, AUDIT_ID, REPORT_ID)
        ).isInstanceOf(ReportNotFoundException.class);

        verify(fileStorageService, never())
                .download(anyString());
    }

    private void givenOwnedReport(
            ReportFormat format,
            String s3Key
    ) {
        given(reportRepository
                .findByIdAndAudit_IdAndAudit_User_IdAndReportType(
                        REPORT_ID,
                        AUDIT_ID,
                        USER_ID,
                        ReportType.HIGH_IMPACT_REPORT
                ))
                .willReturn(Optional.of(report));

        given(report.getFormat()).willReturn(format);
        given(report.getFilePath()).willReturn(s3Key);
    }

    @Test
    void rejectsLatestReportNotOwnedByUser() {
        given(reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        AUDIT_ID,
                        USER_ID,
                        ReportType.HIGH_IMPACT_REPORT,
                        ReportFormat.PDF
                ))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getLatest(
                        USER_ID,
                        AUDIT_ID,
                        ReportFormat.PDF
                )
        ).isInstanceOf(ReportNotFoundException.class);

        verify(fileStorageService, never())
                .download(anyString());
    }
}
