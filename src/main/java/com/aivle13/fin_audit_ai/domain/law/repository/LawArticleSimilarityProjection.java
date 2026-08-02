package com.aivle13.fin_audit_ai.domain.law.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 유사도 점수를 함께 받는 조항 검색 결과.
 *
 * <p>엔티티만 돌려주는 조회로는 "얼마나 비슷해서 인용됐는지"를 알 수 없어, 인용 증적에
 * 담을 점수를 함께 받는다.
 */
public interface LawArticleSimilarityProjection {

    Long getArticleId();

    String getLawName();

    String getArticleNo();

    String getSummary();

    String getContent();

    LocalDate getRevisionDate();

    BigDecimal getSimilarity();
}
