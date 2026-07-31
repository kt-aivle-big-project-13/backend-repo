package com.aivle13.fin_audit_ai.domain.dashboard.repository.projection;

public interface DashboardSummaryProjection {

    Long getAnalyzedModelCount();

    Long getNormalModelCount();

    Long getReviewRequiredCount();

    Long getThresholdExceededCount();
}
