package com.aivle13.fin_audit_ai.domain.report.service.common;

import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;

public record ReportDownloadResult(
        DownloadedFile file,
        String fileName
) {
}