package com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness;

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
        Performance performance,
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
            @JsonProperty("proportional_parity_ratio") BigDecimal proportionalParityRatio,
            @JsonProperty("fpr_parity_difference") BigDecimal fprParityDifference,
            @JsonProperty("fdr_parity_difference") BigDecimal fdrParityDifference,
            @JsonProperty("for_parity_difference") BigDecimal forParityDifference,
            @JsonProperty("fnr_parity_difference") BigDecimal fnrParityDifference,
            List<GroupStat> groups,
            @JsonProperty("excluded_groups") List<String> excludedGroups,
            String note
    ) {
    }

    public record GroupStat(
            String group,
            int n,
            @JsonProperty("approval_rate") BigDecimal approvalRate,
            @JsonProperty("actual_default_rate") BigDecimal actualDefaultRate,
            int tp,
            int fp,
            int tn,
            int fn,
            BigDecimal auc
    ) {
    }

    // 감사셋 전체 모델 판별 성능 (AUC·정확도). 감사 단위 1건.
    public record Performance(
            BigDecimal auc,
            BigDecimal accuracy,
            String note
    ) {
    }

    // 백엔드 FairnessMetricCode 키로 정리된 지표 요약. saveFairnessResult() 는 이 요약이
    // 아니라 fairnessByAttribute 의 상세 필드에서 직접 값을 읽으므로, 이 레코드는 AI 서버
    // 응답을 그대로 역직렬화하기 위한 계약 완결성 목적이 크다.
    public record FairnessMetricValues(
            @JsonProperty("DEMOGRAPHIC_PARITY") BigDecimal demographicParity,
            @JsonProperty("EQUAL_OPPORTUNITY") BigDecimal equalOpportunity,
            @JsonProperty("EQUALIZED_ODDS") BigDecimal equalizedOdds,
            @JsonProperty("PROPORTIONAL_PARITY") BigDecimal proportionalParity,
            @JsonProperty("FPR_PARITY") BigDecimal fprParity,
            @JsonProperty("FDR_PARITY") BigDecimal fdrParity,
            @JsonProperty("FOR_PARITY") BigDecimal forParity,
            @JsonProperty("FNR_PARITY") BigDecimal fnrParity
    ) {
    }

    public record ValidationIssue(
            String level,
            String item,
            String message
    ) {
    }
}
