package com.aivle13.fin_audit_ai.domain.report.service.improvement;

import com.aivle13.fin_audit_ai.domain.report.dto.response.improvement.ImprovementGuideMetadataResponse;
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
public class ImprovementGuideQueryService {

    private final ReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public ImprovementGuideMetadataResponse getLatest(
            Long userId,
            Long auditId,
            ReportFormat format
    ) {
        ReportEntity report = reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeAndFormatOrderByCreatedAtDescIdDesc(
                        auditId,
                        userId,
                        ReportType.IMPROVEMENT_GUIDE,
                        format
                )
                .orElseThrow(ReportNotFoundException::new);

        return ImprovementGuideMetadataResponse.from(report);
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
                        ReportType.IMPROVEMENT_GUIDE
                )
                .orElseThrow(ReportNotFoundException::new);

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

    private String createFilename(
            Long auditId,
            ReportFormat format
    ) {
        String extension = switch (format) {
            case HTML -> "html";
            case PDF -> "pdf";
            case WORD -> "docx";
        };

        return "improvement-guide-%d.%s".formatted(
                auditId,
                extension
        );
    }

    private String resolveContentType(
            ReportFormat format
    ) {
        return switch (format) {
            case HTML -> "text/html; charset=UTF-8";
            case PDF -> "application/pdf";
            case WORD ->
                    "application/vnd.openxmlformats-officedocument"
                            + ".wordprocessingml.document";
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
