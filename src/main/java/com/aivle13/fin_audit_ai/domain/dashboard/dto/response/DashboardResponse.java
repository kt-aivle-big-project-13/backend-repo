package com.aivle13.fin_audit_ai.domain.dashboard.dto.response;

import java.util.List;

public record DashboardResponse(
        DashboardSummaryResponse summary,
        AuditResultDistributionResponse auditResultDistribution,
        List<ReviewRequiredModelResponse> reviewRequiredTopModels,
        List<FairnessMetricDistributionResponse> fairnessMetricDistributions,
        List<RecentAuditResponse> recentAudits
) {
}
