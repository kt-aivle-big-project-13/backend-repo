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

    // 법령 개정 감지 시 조문 원문을 최신화한다. summary는 새 content를 반영하지 못하는
    // 낡은 텍스트가 되므로 호출부에서 재요약해 updateSummary로 다시 채워야 하고, embedding은
    // 여기서 비워 LawArticleEmbeddingService.embedMissingArticles()가 재임베딩 대상으로
    // 자동으로 잡게 한다.
    public void applyRevision(String content, LocalDate revisionDate) {
        this.content = content;
        this.revisionDate = revisionDate;
        this.embedding = null;
    }

    // 조문변경여부=N이라 개정 반영은 건너뛰지만, 비교 기준일은 이 시행일자로 갱신해둔다.
    // 안 그러면 다음 배치에서 같은 날짜로 오는 후속 정정 응답(내용은 다르지만 changed=N)을
    // "시행일자가 다르다"는 이유로 영영 놓치게 된다.
    public void acknowledgeEffectiveDate(LocalDate effectiveDate) {
        this.revisionDate = effectiveDate;
    }
}
