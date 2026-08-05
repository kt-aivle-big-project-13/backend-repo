package com.aivle13.fin_audit_ai.global.ai.dto.report.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * 개선 권고 가이드 생성 요청.
 *
 * <p>규제준수 판정서와 같이 근거를 백엔드가 조립해 보낸다. 새로 판정하지 않고, 임계값
 * 판정이 끝나 저장된 결과 중 조치가 필요한 항목만 담는다. 우선순위는 AI 서버가 코드로
 * 산정하므로 여기서는 값과 상태만 전달한다.
 */
public record ImprovementGuideRequest(

        @JsonProperty("audit_id")
        Long auditId,

        @JsonProperty("audit_name")
        String auditName,

        @JsonProperty("model_name")
        String modelName,

        @JsonProperty("compliance_gaps")
        List<ComplianceGap> complianceGaps,

        @JsonProperty("self_check_gaps")
        List<SelfCheckGap> selfCheckGaps,

        // 노력의무 문항(IA-01, IA-02, SC-04)에서 '아니오'로 답한 것들 — 위반이 아니라 권장
        // 사항이라 개선 권고(self_check_gaps)와 섞이지 않도록 별도로 보낸다. AI 서버가
        // 아직 이 필드를 모르면(스키마 미반영) 조용히 무시되고, 그때까지는 참고 권고
        // 섹션이 report_narrative에 반영되지 않는다 — AI 레포 쪽 스키마 반영은 별도 진행.
        @JsonProperty("self_check_recommendations")
        List<SelfCheckGap> selfCheckRecommendations,

        @JsonProperty("fairness_findings")
        List<MetricFinding> fairnessFindings,

        @JsonProperty("explainability_findings")
        List<MetricFinding> explainabilityFindings
) {

    /** 미준수로 판정된 법령 조항. */
    public record ComplianceGap(

            @JsonProperty("law_name")
            String lawName,

            @JsonProperty("article_no")
            String articleNo,

            String summary,

            String evidence
    ) {
    }

    /** 자가점검에서 '아니오'로 답한 항목. */
    public record SelfCheckGap(

            @JsonProperty("item_code")
            String itemCode,

            String label
    ) {
    }

    /**
     * 임계값을 넘어 조치가 필요한 지표.
     *
     * <p>공정성은 보호속성별로 산출되므로 {@code attribute} 가 있고, 설명가능성은 모델
     * 전체 단위라 비어 있다.
     */
    public record MetricFinding(

            String attribute,

            @JsonProperty("metric_code")
            String metricCode,

            BigDecimal value,

            BigDecimal threshold,

            String status
    ) {
    }
}
