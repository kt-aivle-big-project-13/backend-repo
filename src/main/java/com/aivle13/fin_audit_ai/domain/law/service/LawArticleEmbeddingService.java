package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.global.ai.client.EmbeddingClient;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * summary는 있지만 embedding이 비어있는 조항을 대상으로 AI 서버에 임베딩 생성을 요청해 채운다.
 * LawArticleSeedService와 동일하게 이번 실행 전체를 하나의 트랜잭션으로 묶되, 조항별 AI 서버
 * 오류는 여기서 잡아 배치를 중단시키지 않는다. 처리하지 못한 조항은 embedding이 계속 비어있어
 * findByEmbeddingIsNull()에 다시 걸리므로, 재실행만으로 안전하게 재시도된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LawArticleEmbeddingService {

    private final LawArticleRepository lawArticleRepository;
    private final EmbeddingClient embeddingClient;

    @Transactional(rollbackFor = Exception.class)
    public int embedMissingArticles() {
        List<LawArticleEntity> targets = lawArticleRepository.findByEmbeddingIsNull();
        int embedded = 0;

        for (LawArticleEntity article : targets) {
            String summary = article.getSummary();

            if (summary == null || summary.isBlank()) {
                log.warn(
                        "summary가 없어 임베딩을 건너뜁니다: lawName={}, articleNo={}",
                        article.getLawName(),
                        article.getArticleNo()
                );
                continue;
            }

            try {
                article.updateEmbedding(embeddingClient.embed(summary));
                embedded++;
            } catch (BusinessException exception) {
                log.error(
                        "조항 임베딩 실패: lawName={}, articleNo={}, errorCode={}",
                        article.getLawName(),
                        article.getArticleNo(),
                        exception.getErrorCode().getCode(),
                        exception
                );
            }
        }

        return embedded;
    }
}
