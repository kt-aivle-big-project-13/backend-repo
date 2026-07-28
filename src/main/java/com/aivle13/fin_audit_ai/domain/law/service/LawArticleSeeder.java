package com.aivle13.fin_audit_ai.domain.law.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 로컬 개발 환경에서 법령 조항 원문(엑셀 기반 CSV)을 law_articles 테이블에 시딩한다.
 * 파일별로 LawArticleSeedService가 트랜잭션 단위로 처리하며, 이미 존재하는 조항은 건너뛰므로
 * 재기동해도 안전하게 다시 실행할 수 있다.
 * LawArticleEmbeddingSeeder(임베딩 채우기)가 시딩된 행을 대상으로 하므로 이 러너보다 먼저 실행되어야 한다.
 */
@Slf4j
@Component
@Profile("dev")
@Order(0)
@RequiredArgsConstructor
public class LawArticleSeeder implements ApplicationRunner {

    private static final List<String> SEED_FILES = List.of(
            "db/seed/ai-basic-act.csv",
            "db/seed/ai-basic-act-decree.csv"
    );

    private final LawArticleSeedService lawArticleSeedService;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (String seedFile : SEED_FILES) {
            int inserted = lawArticleSeedService.seedFrom(seedFile);
            log.info("법령 조항 시딩 확인: {} (신규 {}건)", seedFile, inserted);
        }
    }
}
