package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.global.lawapi.config.LawApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 법령 개정 감지를 주기적으로 트리거만 하는 얇은 컴포넌트. 실제 감지·반영 로직은
 * {@link LawRevisionDetectionService}에 있어 스케줄링 없이도 단위 테스트할 수 있다.
 * law.go.kr OC 키가 아직 없거나(app.law-api.enabled=false) 발급 전이면 매 실행마다
 * API 호출을 시도해 실패하는 것을 막기 위해 스킵한다 — LawArticleEmbeddingSeeder와 동일한 패턴.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LawRevisionScheduler {

    private final LawRevisionDetectionService lawRevisionDetectionService;
    private final LawApiProperties lawApiProperties;

    @Scheduled(cron = "${app.law-revision-job.cron:0 0 6 * * *}")
    public void run() {
        if (!lawApiProperties.enabled()) {
            log.warn("법령 API 비활성화로 법령 개정 감지를 건너뜁니다.");
            return;
        }

        int revisionCount = lawRevisionDetectionService.detectAndApply().size();
        log.info("법령 개정 감지 완료: 반영 {}건", revisionCount);
    }
}
