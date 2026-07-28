package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.global.ai.client.EmbeddingClient;
import com.pgvector.PGvector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LawArticleSearchServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private LawArticleRepository lawArticleRepository;

    @Mock
    private LawArticleEntity article;

    @InjectMocks
    private LawArticleSearchService lawArticleSearchService;

    @Test
    void embedsQueryTextAndReturnsTopKSimilarArticles() {
        float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
        given(embeddingClient.embed("고위험 신용평가 모델")).willReturn(queryEmbedding);

        String expectedLiteral = new PGvector(queryEmbedding).toString();
        given(lawArticleRepository.findTopKBySimilarity(expectedLiteral, 5))
                .willReturn(List.of(article));

        List<LawArticleEntity> result =
                lawArticleSearchService.searchSimilarArticles("고위험 신용평가 모델", 5);

        assertThat(result).containsExactly(article);
        verify(lawArticleRepository).findTopKBySimilarity(expectedLiteral, 5);
    }
}
