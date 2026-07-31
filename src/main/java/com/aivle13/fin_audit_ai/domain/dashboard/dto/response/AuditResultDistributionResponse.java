package com.aivle13.fin_audit_ai.domain.dashboard.dto.response;

public record AuditResultDistributionResponse(
        long totalCount,
        long normalCount,
        long reviewRequiredCount,
        long thresholdExceededCount
) {
}
