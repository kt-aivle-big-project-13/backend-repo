package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.dto.HighImpactReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.exception.report.ReportNotFoundException;
import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class HighImpactReportQueryService {

    private final ReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public HighImpactReportMetadataResponse getLatest(
            Long userId,
            Long auditId,
            ReportFormat format
    ) {
        validateFormat(format);

        ReportEntity report = reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        auditId,
                        userId,
                        ReportType.HIGH_IMPACT_REPORT,
                        format
                )
                .orElseThrow(ReportNotFoundException::new);

        return HighImpactReportMetadataResponse.from(report);
    }

    public ReportDownload download(
            Long userId,
            Long auditId,
            Long reportId
    ) {
        ReportEntity report = reportRepository
                .findByIdAndAudit_IdAndAudit_User_IdAndReportType(
                        reportId,
                        auditId,
                        userId,
                        ReportType.HIGH_IMPACT_REPORT
                )
                .orElseThrow(ReportNotFoundException::new);

        validateFormat(report.getFormat());

        DownloadedFile file = fileStorageService.download(
                report.getFilePath()
        );

        return new ReportDownload(
                createFilename(auditId, report.getFormat()),
                resolveContentType(report.getFormat()),
                file.contentLength(),
                file.content()
        );
    }

    private void validateFormat(ReportFormat format) {
        if (format != ReportFormat.PDF
                && format != ReportFormat.WORD) {
            throw new ReportNotFoundException();
        }
    }

    private String createFilename(
            Long auditId,
            ReportFormat format
    ) {
        String extension = switch (format) {
            case PDF -> "pdf";
            case WORD -> "docx";
            case HTML -> throw new ReportNotFoundException();
        };

        return "high-impact-assessment-%d.%s".formatted(
                auditId,
                extension
        );
    }

    private String resolveContentType(
            ReportFormat format
    ) {
        return switch (format) {
            case PDF -> "application/pdf";
            case WORD ->
                    "application/vnd.openxmlformats-officedocument"
                            + ".wordprocessingml.document";
            case HTML -> throw new ReportNotFoundException();
        };
    }

    public record ReportDownload(
            String filename,
            String contentType,
            long size,
            InputStream content
    ) {
    }
}
