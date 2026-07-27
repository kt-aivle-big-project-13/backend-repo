package com.aivle13.fin_audit_ai.domain.law.entity;

import com.aivle13.fin_audit_ai.global.entity.VectorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

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

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Type(VectorType.class)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "revision_date")
    private LocalDate revisionDate;

    public static LawArticleEntity of(String lawName, String articleNo, String content, LocalDate effectiveDate) {
        LawArticleEntity entity = new LawArticleEntity();
        entity.lawName = lawName;
        entity.articleNo = articleNo;
        entity.content = content;
        entity.effectiveDate = effectiveDate;
        return entity;
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }

    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
