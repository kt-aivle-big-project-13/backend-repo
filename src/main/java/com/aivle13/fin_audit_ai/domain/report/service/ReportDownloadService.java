package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportDownloadService {

    private final ReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    public ReportDownloadResult download(Long reportId) {
        ReportEntity report = reportRepository.findById(reportId)
                .filter(entity ->
                        entity.getReportType()
                                == ReportType.FINAL_AUDIT_REPORT
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "최종 감사 보고서를 찾을 수 없습니다."
                        )
                );

        DownloadedFile downloadedFile = fileStorageService.download(report.getFilePath());

        String fileName = createFileName(report);

        return new ReportDownloadResult(downloadedFile, fileName);
    }

    private String createFileName(ReportEntity report) {
        return switch (report.getFormat()) {
            case PDF ->
                    "final-audit-report-%d.pdf"
                            .formatted(report.getAudit().getId());

            case WORD ->
                    "final-audit-report-%d.docx"
                            .formatted(report.getAudit().getId());

            case HTML -> "final-audit-report-%d.html"
                    .formatted(report.getAudit().getId());
        };
    }
}