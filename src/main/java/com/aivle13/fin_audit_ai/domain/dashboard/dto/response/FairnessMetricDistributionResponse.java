package com.aivle13.fin_audit_ai.domain.dashboard.dto.response;

import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessMetricCode;

public record FairnessMetricDistributionResponse(
        FairnessMetricCode metricCode,
        long passCount,
        long reviewCount,
        long failCount,
        long unavailableCount,
        double passRate,
        double reviewRate,
        double failRate,
        double unavailableRate
) {
}
