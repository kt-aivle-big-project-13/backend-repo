package com.aivle13.fin_audit_ai.domain.audit.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ExplainabilityResultRequestDto(

        @JsonProperty("pipeline_status")
        String pipelineStatus,

        @JsonProperty("overall_status")
        String overallStatus,

        @JsonProperty("key_metrics")
        KeyMetrics keyMetrics
) {

    public record KeyMetrics(

            @JsonProperty("sensitive_contribution_ratio")
            Metric sensitiveContributionRatio,

            @JsonProperty("global_explanation_stability")
            Metric globalExplanationStability,

            @JsonProperty("explanation_fidelity")
            Metric explanationFidelity
    ) {
    }

    public record Metric(
            String metric,
            String label,
            BigDecimal value,
            BigDecimal threshold,
            String status
    ) {
    }
}