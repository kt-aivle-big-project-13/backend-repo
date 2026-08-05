package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleSimilarityProjection;
import com.aivle13.fin_audit_ai.global.ai.client.chat.EmbeddingClient;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.request.ChatAnswerRequest;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 질문과 관련된 법령 조항을 찾아 답변 근거로 넘긴다.
 *
 * <p>유사도가 낮은 조항까지 근거로 주면 답변이 엉뚱한 법령을 인용하게 되므로, 하한
 * 미만은 버린다. 관련 조항이 없으면 법령 인용 없이 감사 수치만으로 답한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatLawSearchService {

    private final EmbeddingClient embeddingClient;
    private final LawArticleRepository lawArticleRepository;

    @Value("${app.chat.law-top-k:3}")
    private int topK;

    @Value("${app.chat.law-min-similarity:0.30}")
    private BigDecimal minSimilarity;

    @Transactional(readOnly = true)
    public List<ChatAnswerRequest.LawArticle> search(String question) {
        try {
            float[] embedding = embeddingClient.embed(question);

            return lawArticleRepository
                    .findTopKWithSimilarity(
                            new PGvector(embedding).toString(),
                            topK
                    )
                    .stream()
                    .filter(this::isRelevant)
                    .map(ChatLawSearchService::toLawArticle)
                    .toList();
        } catch (RuntimeException exception) {
            // 법령 검색은 답변의 보조 근거다. 실패해도 감사 수치만으로 답할 수 있으므로
            // 질문 처리 전체를 막지 않는다.
            log.warn("법령 조항 검색에 실패해 법령 인용 없이 답변합니다.", exception);
            return List.of();
        }
    }

    private boolean isRelevant(LawArticleSimilarityProjection projection) {
        BigDecimal similarity = projection.getSimilarity();

        return similarity != null
                && similarity.compareTo(minSimilarity) >= 0;
    }

    private static ChatAnswerRequest.LawArticle toLawArticle(
            LawArticleSimilarityProjection projection
    ) {
        // law_articles 에 원문 링크 컬럼이 없어 source_url 은 비워 둔다. 링크를
        // 문자열로 조립하면 깨진 주소가 나갈 수 있다.
        return new ChatAnswerRequest.LawArticle(
                projection.getLawName(),
                projection.getArticleNo(),
                projection.getSummary(),
                projection.getContent(),
                projection.getSimilarity(),
                null,
                projection.getRevisionDate() != null
        );
    }
}
