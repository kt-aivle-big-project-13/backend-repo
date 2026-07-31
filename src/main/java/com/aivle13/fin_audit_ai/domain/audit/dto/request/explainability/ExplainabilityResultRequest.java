package com.aivle13.fin_audit_ai.domain.audit.dto.request.explainability;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

// AI 서버는 include_report=true일 때 report·artifacts·artifacts_status 확장 필드를 추가로 내려주는데,
// 여기서 쓰는 건 report.global_importance_top뿐이라 나머지 미모델링 필드는 전부 무시한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExplainabilityResultRequest(

        @JsonProperty("pipeline_status")
        String pipelineStatus,

        @JsonProperty("overall_status")
        String overallStatus,

        @JsonProperty("key_metrics")
        KeyMetrics keyMetrics,

        // include_report=false로 요청했을 때는 응답에 아예 안 실려서 null일 수 있다.
        ShapReport report
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShapReport(
            @JsonProperty("global_importance_top")
            List<FeatureImportance> globalImportanceTop
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeatureImportance(
            Integer rank,
            String feature,

            @JsonProperty("mean_abs_shap")
            BigDecimal meanAbsShap,

            @JsonProperty("mean_signed_shap")
            BigDecimal meanSignedShap,

            @JsonProperty("contribution_ratio")
            BigDecimal contributionRatio,

            String direction,

            @JsonProperty("is_sensitive")
            Boolean isSensitive,

            @JsonProperty("sensitive_group")
            String sensitiveGroup
    ) {
    }
}