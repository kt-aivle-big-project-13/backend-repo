package com.aivle13.fin_audit_ai.global.lawapi.client;

import java.time.LocalDate;

/**
 * law.go.kr 응답을 우리 도메인(LawArticleEntity)과 비교 가능한 형태로 정규화한 결과.
 * articleNo는 law_articles.article_no와 동일한 "제N조"/"제N조의M" 형식이다.
 */
public record LawArticleRevision(
        String articleNo,
        String content,
        LocalDate effectiveDate
) {
}
