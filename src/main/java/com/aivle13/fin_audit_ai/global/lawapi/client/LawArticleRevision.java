package com.aivle13.fin_audit_ai.global.lawapi.client;

import java.time.LocalDate;

/**
 * law.go.kr 응답을 우리 도메인(LawArticleEntity)과 비교 가능한 형태로 정규화한 결과.
 * articleNo는 law_articles.article_no와 동일한 "제N조"/"제N조의M" 형식이다.
 *
 * <p>law.go.kr이 내려주는 "조문변경여부"는 전부개정처럼 내용은 그대로인데 시행일자만
 * 갱신되는 경우에도 Y로 서기도 하고, 실제로 내용이 바뀐 정정에도 N으로 오는 경우가 있어
 * 신뢰할 수 없는 값이라 여기서 아예 담지 않는다({@code LawRevisionApplier}가 개정 여부를
 * content 실제 비교로만 판단한다).
 */
public record LawArticleRevision(
        String articleNo,
        String content,
        LocalDate effectiveDate
) {
}
