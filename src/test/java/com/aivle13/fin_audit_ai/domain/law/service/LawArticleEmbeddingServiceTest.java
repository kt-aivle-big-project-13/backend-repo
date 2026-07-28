package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.global.ai.client.EmbeddingClient;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LawArticleEmbeddingServiceTest {

    @Mock
    private LawArticleRepository lawArticleRepository;

    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private LawArticleEmbeddingService lawArticleEmbeddingService;

    @Test
    void embedsArticlesThatHaveSummary() {
        LawArticleEntity article = articleWithSummary("제1조", "목적 요약");

        given(lawArticleRepository.findByEmbeddingIsNull())
                .willReturn(List.of(article));

        float[] embedding = {0.1f, 0.2f, 0.3f};
        given(embeddingClient.embed("목적 요약")).willReturn(embedding);

        int embedded = lawArticleEmbeddingService.embedMissingArticles();

        assertThat(embedded).isEqualTo(1);
        assertThat(article.getEmbedding()).containsExactly(embedding);
    }

    @Test
    void skipsArticlesWithoutSummary() {
        LawArticleEntity article = articleWithSummary("제2조", null);

        given(lawArticleRepository.findByEmbeddingIsNull())
                .willReturn(List.of(article));

        int embedded = lawArticleEmbeddingService.embedMissingArticles();

        assertThat(embedded).isEqualTo(0);
        assertThat(article.getEmbedding()).isNull();
        verify(embeddingClient, never()).embed(anyString());
    }

    @Test
    void continuesRemainingArticlesWhenOneFailsWithAiServerError() {
        LawArticleEntity failing = articleWithSummary("제3조", "실패할 요약");
        LawArticleEntity succeeding = articleWithSummary("제4조", "성공할 요약");

        given(lawArticleRepository.findByEmbeddingIsNull())
                .willReturn(List.of(failing, succeeding));

        given(embeddingClient.embed("실패할 요약"))
                .willThrow(new AiServerErrorException());

        float[] embedding = {0.4f, 0.5f};
        given(embeddingClient.embed("성공할 요약")).willReturn(embedding);

        int embedded = lawArticleEmbeddingService.embedMissingArticles();

        assertThat(embedded).isEqualTo(1);
        assertThat(failing.getEmbedding()).isNull();
        assertThat(succeeding.getEmbedding()).containsExactly(embedding);
    }

    private LawArticleEntity articleWithSummary(String articleNo, String summary) {
        LawArticleEntity article = LawArticleEntity.of(
                "인공지능 기본법",
                articleNo,
                "조문 원문",
                LocalDate.of(2026, 1, 22)
        );

        if (summary != null) {
            article.updateSummary(summary);
        }

        return article;
    }
}
