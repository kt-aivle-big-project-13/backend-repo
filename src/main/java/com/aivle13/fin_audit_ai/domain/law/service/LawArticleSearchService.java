package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.global.ai.client.EmbeddingClient;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 질의 텍스트를 임베딩해 law_articles 중 pgvector 코사인 거리가 가장 가까운 조항 top-K를 찾는다.
 */
@Service
@RequiredArgsConstructor
public class LawArticleSearchService {

    private final EmbeddingClient embeddingClient;
    private final LawArticleRepository lawArticleRepository;

    public List<LawArticleEntity> searchSimilarArticles(String queryText, int topK) {
        if (topK <= 0) {
            throw new IllegalArgumentException(
                    "topK는 1 이상이어야 합니다: " + topK
            );
        }

        float[] queryEmbedding = embeddingClient.embed(queryText);

        return lawArticleRepository.findTopKBySimilarity(
                new PGvector(queryEmbedding).toString(),
                topK
        );
    }
}
