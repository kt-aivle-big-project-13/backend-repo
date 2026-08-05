package com.aivle13.fin_audit_ai.domain.report.dto.response.common;

import java.util.List;

public record ReportGenerationResponse(
        Long auditId,
        List<GeneratedReportResponse> reports
) {
}