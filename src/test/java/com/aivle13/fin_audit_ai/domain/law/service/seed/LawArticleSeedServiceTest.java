package com.aivle13.fin_audit_ai.domain.law.service.seed;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.global.exception.law.LawApiErrorException;
import com.aivle13.fin_audit_ai.global.lawapi.client.LawApiClient;
import com.aivle13.fin_audit_ai.global.lawapi.client.LawArticleRevision;
import com.aivle13.fin_audit_ai.global.lawapi.config.LawApiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 시딩 content가 CSV 원문이 아니라 law.go.kr API를 통해 저장되는지 검증한다.
 * 이게 어긋나면 {@link com.aivle13.fin_audit_ai.domain.law.service.revision.LawRevisionApplier}가
 * 개정 감지 때 비교하는 API content와 포맷이 달라져, 실제로는 안 바뀐 조문이 개정으로
 * 오탐된다(2026-08-10 발생 인시던트).
 */
@ExtendWith(MockitoExtension.class)
class LawArticleSeedServiceTest {

    private static final String SEED_FILE = "db/seed/ai-basic-act.csv";
    private static final String OFFICIAL_LAW_NAME = "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법";
    private static final String LAW_NAME = "AI 기본법";

    @Mock
    private LawArticleRepository lawArticleRepository;

    @Mock
    private LawApiClient lawApiClient;

    @Test
    void usesApiFlattenedContentInsteadOfCsvContentWhenApiAvailable() throws Exception {
        given(lawApiClient.fetchArticles(OFFICIAL_LAW_NAME)).willReturn(List.of(
                new LawArticleRevision("제1조", "API가 평탄화한 조문 텍스트", LocalDate.of(2026, 1, 22))
        ));

        LawArticleSeedService seedService =
                new LawArticleSeedService(lawArticleRepository, lawApiClient, enabledProperties());

        int inserted = seedService.seedFrom(SEED_FILE, OFFICIAL_LAW_NAME);

        ArgumentCaptor<LawArticleEntity> captor = ArgumentCaptor.forClass(LawArticleEntity.class);
        verify(lawArticleRepository, times(inserted)).saveAndFlush(captor.capture());

        LawArticleEntity firstArticle = captor.getAllValues().stream()
                .filter(entity -> "제1조".equals(entity.getArticleNo()))
                .findFirst()
                .orElseThrow();

        assertThat(firstArticle.getLawName()).isEqualTo(LAW_NAME);
        assertThat(firstArticle.getContent()).isEqualTo("API가 평탄화한 조문 텍스트");
    }

    @Test
    void fallsBackToCsvContentWhenApiDisabled() throws Exception {
        LawArticleSeedService seedService =
                new LawArticleSeedService(lawArticleRepository, lawApiClient, disabledProperties());

        int inserted = seedService.seedFrom(SEED_FILE, OFFICIAL_LAW_NAME);

        ArgumentCaptor<LawArticleEntity> captor = ArgumentCaptor.forClass(LawArticleEntity.class);
        verify(lawArticleRepository, times(inserted)).saveAndFlush(captor.capture());

        LawArticleEntity firstArticle = captor.getAllValues().stream()
                .filter(entity -> "제1조".equals(entity.getArticleNo()))
                .findFirst()
                .orElseThrow();

        assertThat(firstArticle.getContent()).startsWith("제1조(목적)");
        verify(lawApiClient, never()).fetchArticles(anyString());
    }

    @Test
    void skipsApiCallWhenAllArticlesAlreadySeeded() throws Exception {
        // 이미 다 시딩된 상태로 재기동해도(existsByLawNameAndArticleNo가 매번 걸러냄) 매 실행마다
        // law.go.kr을 두드리면 재기동할 때마다 불필요한 API 호출이 나간다. 신규로 넣을 행이
        // 하나도 없으면 API 자체를 호출하지 않아야 한다.
        given(lawArticleRepository.existsByLawNameAndArticleNo(anyString(), anyString())).willReturn(true);

        LawArticleSeedService seedService =
                new LawArticleSeedService(lawArticleRepository, lawApiClient, enabledProperties());

        int inserted = seedService.seedFrom(SEED_FILE, OFFICIAL_LAW_NAME);

        assertThat(inserted).isZero();
        verify(lawApiClient, never()).fetchArticles(anyString());
        verify(lawArticleRepository, never()).saveAndFlush(any());
    }

    @Test
    void fallsBackToCsvContentWhenApiCallFails() throws Exception {
        given(lawApiClient.fetchArticles(OFFICIAL_LAW_NAME)).willThrow(new LawApiErrorException());

        LawArticleSeedService seedService =
                new LawArticleSeedService(lawArticleRepository, lawApiClient, enabledProperties());

        int inserted = seedService.seedFrom(SEED_FILE, OFFICIAL_LAW_NAME);

        ArgumentCaptor<LawArticleEntity> captor = ArgumentCaptor.forClass(LawArticleEntity.class);
        verify(lawArticleRepository, times(inserted)).saveAndFlush(captor.capture());

        LawArticleEntity firstArticle = captor.getAllValues().stream()
                .filter(entity -> "제1조".equals(entity.getArticleNo()))
                .findFirst()
                .orElseThrow();

        assertThat(firstArticle.getContent()).startsWith("제1조(목적)");
    }

    private LawApiProperties enabledProperties() {
        return new LawApiProperties(true, "test-oc-key", URI.create("http://www.law.go.kr"),
                Duration.ofSeconds(3), Duration.ofSeconds(3));
    }

    private LawApiProperties disabledProperties() {
        return new LawApiProperties(false, "test-oc-key", URI.create("http://www.law.go.kr"),
                Duration.ofSeconds(3), Duration.ofSeconds(3));
    }
}
