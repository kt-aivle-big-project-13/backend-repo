package com.aivle13.fin_audit_ai.domain.audit.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI 서버 {@code POST /api/fairness/audits} 응답 (AuditRunResponse 미러링).
 * method/status/calibrationSource/level은 AI 쪽 enum과 우리 쪽 enum의 값이 서로 달라
 * (예: method는 "target_approval_rate"/"manual", 우리는 VALIDATION_DATASET/MANUAL)
 * 섣불리 우리 enum으로 매핑하지 않고 문자열 그대로 받는다.
 */
public record FairnessRunResponse(
        @JsonProperty("audit_id") String auditId,
        @JsonProperty("audit_name") String auditName,
        ThresholdInfo threshold,
        @JsonProperty("n_customers") int nCustomers,
        @JsonProperty("approval_rate") BigDecimal approvalRate,
        @JsonProperty("calibration_source") String calibrationSource,
        @JsonProperty("fairness_by_attribute") Map<String, AttributeFairness> fairnessByAttribute,
        @JsonProperty("fairness_summary") Map<String, FairnessMetricValues> fairnessSummary,
        List<ValidationIssue> warnings
) {

    public record ThresholdInfo(
            BigDecimal value,
            String method,
            String basis,
            @JsonProperty("computed_from") String computedFrom
    ) {
    }

    public record AttributeFairness(
            String attribute,
            String status,
            @JsonProperty("demographic_parity_difference") BigDecimal demographicParityDifference,
            @JsonProperty("equal_opportunity_difference") BigDecimal equalOpportunityDifference,
            @JsonProperty("equalized_odds_difference") BigDecimal equalizedOddsDifference,
            List<GroupStat> groups,
            @JsonProperty("excluded_groups") List<String> excludedGroups,
            String note
    ) {
    }

    public record GroupStat(
            String group,
            int n,
            @JsonProperty("approval_rate") BigDecimal approvalRate,
            @JsonProperty("actual_default_rate") BigDecimal actualDefaultRate
    ) {
    }

    // 백엔드 FairnessMetricCode 키로 정리된 지표 요약 (DEMOGRAPHIC_PARITY/EQUAL_OPPORTUNITY/EQUALIZED_ODDS)
    public record FairnessMetricValues(
            @JsonProperty("DEMOGRAPHIC_PARITY") BigDecimal demographicParity,
            @JsonProperty("EQUAL_OPPORTUNITY") BigDecimal equalOpportunity,
            @JsonProperty("EQUALIZED_ODDS") BigDecimal equalizedOdds
    ) {
    }

    public record ValidationIssue(
            String level,
            String item,
            String message
    ) {
    }
}
