package com.aivle13.fin_audit_ai.domain.law.repository;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LawArticleRepository extends JpaRepository<LawArticleEntity, Long> {

    boolean existsByLawNameAndArticleNo(String lawName, String articleNo);

    Optional<LawArticleEntity> findByLawNameAndArticleNo(String lawName, String articleNo);

    List<LawArticleEntity> findByEmbeddingIsNull();

    /**
     * pgvector 코사인 거리(<=>) 기준으로 가장 가까운(유사한) 조항 top-K를 오름차순(가까운 순)으로 반환한다.
     * queryEmbedding은 "[0.1,0.2,...]" 형식의 pgvector 벡터 리터럴 문자열이다.
     */
    @Query(value = """
            SELECT *
            FROM law_articles
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<LawArticleEntity> findTopKBySimilarity(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit
    );
}
