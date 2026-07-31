package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * 규제준수 판정서 생성 요청.
 *
 * <p>편향·설명가능성 리포트와 달리 S3 Key 만 넘기지 않는다. 판정 근거인 자가점검 응답과
 * 법령 매핑이 백엔드 DB 에만 있어, 여기서 조립해 본문에 담아 보낸다. AI 서버는 판정을
 * 새로 내리지 않고 집계와 서술만 한다.
 */
public record ComplianceReportRequest(

        @JsonProperty("audit_id")
        Long auditId,

        @JsonProperty("audit_name")
        String auditName,

        @JsonProperty("model_name")
        String modelName,

        @JsonProperty("self_check_answers")
        List<SelfCheckAnswer> selfCheckAnswers,

        @JsonProperty("regulation_mappings")
        List<RegulationMapping> regulationMappings,

        @JsonProperty("audit_reference")
        AuditReference auditReference
) {

    /** 자가점검 항목 하나에 대한 응답. 판정의 1차 근거다. */
    public record SelfCheckAnswer(

            @JsonProperty("item_code")
            String itemCode,

            String label,

            boolean answer
    ) {
    }

    /** 자가점검 응답으로 매핑된 법령 조항과 그 준수 판정. */
    public record RegulationMapping(

            @JsonProperty("law_name")
            String lawName,

            @JsonProperty("article_no")
            String articleNo,

            String content,

            String summary,

            String compliance,

            String evidence,

            @JsonProperty("effective_date")
            String effectiveDate,

            @JsonProperty("revision_date")
            String revisionDate
    ) {
    }

    /** 참고용 감사 요약. 판정 근거가 아니라 맥락 제공용이다. */
    public record AuditReference(

            @JsonProperty("n_customers")
            Integer nCustomers,

            @JsonProperty("approval_rate")
            BigDecimal approvalRate,

            BigDecimal threshold,

            @JsonProperty("threshold_method")
            String thresholdMethod,

            BigDecimal auc,

            BigDecimal accuracy,

            List<FairnessReference> fairness
    ) {
    }

    /** 참고용 공정성 지표. 마찬가지로 판정 근거가 아니다. */
    public record FairnessReference(

            String attribute,

            @JsonProperty("demographic_parity_difference")
            BigDecimal demographicParityDifference,

            @JsonProperty("equal_opportunity_difference")
            BigDecimal equalOpportunityDifference,

            @JsonProperty("equalized_odds_difference")
            BigDecimal equalizedOddsDifference,

            @JsonProperty("proportional_parity_ratio")
            BigDecimal proportionalParityRatio
    ) {
    }
}
