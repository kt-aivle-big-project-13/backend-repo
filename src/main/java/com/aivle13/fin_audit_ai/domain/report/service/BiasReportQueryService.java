package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.dto.BiasReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
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
public class BiasReportQueryService {

    private final ReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public BiasReportMetadataResponse getLatest(
            Long userId,
            Long auditId
    ) {
        ReportEntity report = reportRepository
                .findFirstByAudit_IdAndAudit_User_IdAndReportTypeOrderByCreatedAtDescIdDesc(
                        auditId,
                        userId,
                        ReportType.BIAS_REPORT
                )
                .orElseThrow(ReportNotFoundException::new);

        return BiasReportMetadataResponse.from(report);
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
                        ReportType.BIAS_REPORT
                )
                .orElseThrow(ReportNotFoundException::new);

        DownloadedFile file = fileStorageService.download(
                report.getFilePath()
        );

        return new ReportDownload(
                "bias-report-%d.html".formatted(
                        auditId
                ),
                file.contentType(),
                file.contentLength(),
                file.content()
        );
    }

    public record ReportDownload(
            String filename,
            String contentType,
            long size,
            InputStream content
    ) {
    }
}
