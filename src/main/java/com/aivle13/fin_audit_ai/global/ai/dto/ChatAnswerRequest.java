package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * 감사 질의 응답 생성 요청.
 *
 * <p>세 근거는 모두 선택 항목이다. 2단계는 감사 수치만 채우고, 법령은 3단계, 리포트
 * 서술은 5단계에서 채운다. 계약은 처음부터 확정돼 있다.
 */
public record ChatAnswerRequest(

        @JsonProperty("audit_id")
        Long auditId,

        String question,

        @JsonProperty("audit_facts")
        List<AuditFact> auditFacts,

        @JsonProperty("law_articles")
        List<LawArticle> lawArticles,

        @JsonProperty("report_sections")
        List<ReportSection> reportSections
) {

    /** 저장된 감사 수치 하나. */
    public record AuditFact(

            String kind,

            String reference,

            String value,

            String detail
    ) {
    }

    /** pgvector 검색으로 찾은 법령 조항 (3단계). */
    public record LawArticle(

            @JsonProperty("law_name")
            String lawName,

            @JsonProperty("article_no")
            String articleNo,

            String summary,

            String content,

            BigDecimal similarity,

            @JsonProperty("source_url")
            String sourceUrl,

            boolean revised
    ) {
    }

    /** 생성된 리포트의 섹션별 서술 (5단계). */
    public record ReportSection(

            @JsonProperty("report_type")
            String reportType,

            @JsonProperty("section_key")
            String sectionKey,

            String title,

            String content
    ) {
    }
}
