package com.aivle13.fin_audit_ai.domain.dashboard.dto.response;

public record DashboardSummaryResponse(
        long analyzedModelCount,
        long normalModelCount,
        long reviewRequiredCount,
        long thresholdExceededCount,
        double complianceRate
) {
}
