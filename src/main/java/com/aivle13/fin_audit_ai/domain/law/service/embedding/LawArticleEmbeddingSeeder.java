package com.aivle13.fin_audit_ai.domain.law.service.embedding;

import com.aivle13.fin_audit_ai.global.ai.config.AiServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발 환경에서 law_articles 중 embedding이 비어있는 조항을 채운다.
 * LawArticleSeeder(원문 시딩)가 끝난 뒤 실행되어야 하므로 순서를 뒤로 둔다.
 * AI 서버가 꺼져 있으면(app.ai-server.enabled=false) 스킵한다 — 매 재기동마다
 * AI 서버 호출을 시도해 실패하는 것을 막기 위함.
 */
@Slf4j
@Component
@Profile("dev")
@Order(1)
@RequiredArgsConstructor
public class LawArticleEmbeddingSeeder implements ApplicationRunner {

    private final LawArticleEmbeddingService lawArticleEmbeddingService;
    private final AiServerProperties aiServerProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!aiServerProperties.enabled()) {
            log.warn("AI 서버 비활성화로 법령 조항 임베딩 채우기를 건너뜁니다.");
            return;
        }

        int embedded = lawArticleEmbeddingService.embedMissingArticles();
        log.info("법령 조항 임베딩 채우기 완료: 신규 {}건", embedded);
    }
}
