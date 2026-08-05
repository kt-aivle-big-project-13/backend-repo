package com.aivle13.fin_audit_ai.domain.report.dto.response.explainability;

import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;

import java.time.LocalDateTime;

public record ExplainabilityReportMetadataResponse(
        Long reportId,
        Long auditId,
        ReportType reportType,
        ReportFormat format,
        ReportStatus status,
        Integer version,
        LocalDateTime generatedAt
) {

    public static ExplainabilityReportMetadataResponse from(
            ReportEntity report
    ) {
        return new ExplainabilityReportMetadataResponse(
                report.getId(),
                report.getAudit().getId(),
                report.getReportType(),
                report.getFormat(),
                report.getStatus(),
                report.getVersion(),
                report.getCreatedAt()
        );
    }
}
