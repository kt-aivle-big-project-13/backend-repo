package com.aivle13.fin_audit_ai.domain.report.dto.request;

import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;

import java.util.List;

public record ReportGenerationRequest(
        List<ReportFormat> formats
) {

    public ReportGenerationRequest {
        formats = formats == null
                ? List.of()
                : formats.stream()
                .distinct()
                .toList();
    }
}