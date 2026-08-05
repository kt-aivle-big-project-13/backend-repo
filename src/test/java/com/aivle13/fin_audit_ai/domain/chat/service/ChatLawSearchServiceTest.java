package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleSimilarityProjection;
import com.aivle13.fin_audit_ai.global.ai.client.chat.EmbeddingClient;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.request.ChatAnswerRequest;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatLawSearchServiceTest {

    private static final String QUESTION = "이의제기 절차가 법적으로 필요한가요?";

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private LawArticleRepository lawArticleRepository;

    @InjectMocks
    private ChatLawSearchService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "topK", 3);
        ReflectionTestUtils.setField(
                service,
                "minSimilarity",
                new BigDecimal("0.30")
        );
    }

    @Test
    void mapsSearchResultToLawArticles() {
        givenEmbedding();

        // 스터빙 안에서 다시 스터빙하면 UnfinishedStubbingException 이 나므로
        // mock 을 먼저 완성한 뒤 넘긴다.
        LawArticleSimilarityProjection found = projection(
                "신용정보법",
                "제36조의2",
                new BigDecimal("0.8300"),
                LocalDate.of(2024, 3, 1)
        );

        given(lawArticleRepository.findTopKWithSimilarity(anyString(), anyInt()))
                .willReturn(List.of(found));

        List<ChatAnswerRequest.LawArticle> articles = service.search(QUESTION);

        assertThat(articles).hasSize(1);

        ChatAnswerRequest.LawArticle article = articles.get(0);
        assertThat(article.lawName()).isEqualTo("신용정보법");
        assertThat(article.articleNo()).isEqualTo("제36조의2");
        assertThat(article.similarity())
                .isEqualByComparingTo(new BigDecimal("0.8300"));

        // 개정 이력이 있으면 답변에서 주의 표시를 할 수 있도록 알린다.
        assertThat(article.revised()).isTrue();

        // law_articles 에 원문 링크 컬럼이 없어 비워 둔다.
        assertThat(article.sourceUrl()).isNull();
    }

    @Test
    void dropsArticlesBelowSimilarityThreshold() {
        givenEmbedding();

        LawArticleSimilarityProjection relevant = projection(
                "신용정보법", "제36조의2", new BigDecimal("0.8300"), null
        );
        LawArticleSimilarityProjection irrelevant = projection(
                "무관법", "제1조", new BigDecimal("0.1200"), null
        );

        given(lawArticleRepository.findTopKWithSimilarity(anyString(), anyInt()))
                .willReturn(List.of(relevant, irrelevant));

        List<ChatAnswerRequest.LawArticle> articles = service.search(QUESTION);

        // 유사도가 낮은 조항까지 근거로 주면 답변이 엉뚱한 법령을 인용하게 된다.
        assertThat(articles)
                .extracting(ChatAnswerRequest.LawArticle::lawName)
                .containsExactly("신용정보법");
    }

    @Test
    void returnsEmptyWhenNothingIsRelevant() {
        givenEmbedding();

        LawArticleSimilarityProjection irrelevant = projection(
                "무관법", "제1조", new BigDecimal("0.0500"), null
        );

        given(lawArticleRepository.findTopKWithSimilarity(anyString(), anyInt()))
                .willReturn(List.of(irrelevant));

        assertThat(service.search(QUESTION)).isEmpty();
    }

    @Test
    void doesNotBreakQuestionWhenEmbeddingFails() {
        given(embeddingClient.embed(QUESTION))
                .willThrow(new AiServerErrorException());

        // 법령 검색은 보조 근거다. 실패해도 감사 수치만으로 답할 수 있어야 한다.
        assertThat(service.search(QUESTION)).isEmpty();

        verify(lawArticleRepository, never())
                .findTopKWithSimilarity(anyString(), anyInt());
    }

    @Test
    void doesNotBreakQuestionWhenSearchFails() {
        givenEmbedding();

        given(lawArticleRepository.findTopKWithSimilarity(anyString(), anyInt()))
                .willThrow(new IllegalStateException("pgvector 오류"));

        assertThat(service.search(QUESTION)).isEmpty();
    }

    private void givenEmbedding() {
        given(embeddingClient.embed(QUESTION))
                .willReturn(new float[]{0.1f, 0.2f, 0.3f});
    }

    private LawArticleSimilarityProjection projection(
            String lawName,
            String articleNo,
            BigDecimal similarity,
            LocalDate revisionDate
    ) {
        LawArticleSimilarityProjection projection =
                Mockito.mock(LawArticleSimilarityProjection.class);

        given(projection.getSimilarity()).willReturn(similarity);

        if (similarity.compareTo(new BigDecimal("0.30")) >= 0) {
            given(projection.getLawName()).willReturn(lawName);
            given(projection.getArticleNo()).willReturn(articleNo);
            given(projection.getSummary()).willReturn("조항 요지");
            given(projection.getContent()).willReturn("조항 본문");
            given(projection.getRevisionDate()).willReturn(revisionDate);
        }

        return projection;
    }
}
