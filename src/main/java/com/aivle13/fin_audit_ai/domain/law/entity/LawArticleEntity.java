package com.aivle13.fin_audit_ai.domain.law.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 법령 조항 원문. RAG 검색을 위한 임베딩 대상이기도 하다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "law_articles")
public class LawArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_id")
    private Long id;

    @Column(name = "law_name", nullable = false, length = 100)
    private String lawName;

    @Column(name = "article_no", nullable = false, length = 30)
    private String articleNo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /*
     * embedding VECTOR(1536) — pgvector 타입.
     * JPA 기본 타입이 아니므로 아래 중 하나로 처리:
     *  1) pgvector-java (com.pgvector:pgvector) 의존성 + Hibernate 커스텀 타입
     *  2) 임베딩 저장/검색을 네이티브 쿼리로 별도 처리하고, 엔티티에는 매핑 제외
     * 여기서는 매핑 보류(주석) — RAG 파이프라인 붙일 때 확정.
     * 예) @Column(columnDefinition = "vector(1536)")
     *     private float[] embedding;   // 커스텀 타입 등록 후
     */

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "revision_date")
    private LocalDate revisionDate;
}
