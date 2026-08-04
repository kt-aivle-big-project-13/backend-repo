package com.aivle13.fin_audit_ai.global.lawapi.client;

import java.time.LocalDate;

/**
 * law.go.kr 응답을 우리 도메인(LawArticleEntity)과 비교 가능한 형태로 정규화한 결과.
 * articleNo는 law_articles.article_no와 동일한 "제N조"/"제N조의M" 형식이다.
 * changed는 law.go.kr이 내려주는 "조문변경여부"로, 전부개정처럼 법 전체의 시행일자만
 * 갱신되고 내용은 그대로인 조문을 개정으로 오탐하지 않기 위해 필요하다.
 */
public record LawArticleRevision(
        String articleNo,
        String content,
        LocalDate effectiveDate,
        boolean changed
) {
}
