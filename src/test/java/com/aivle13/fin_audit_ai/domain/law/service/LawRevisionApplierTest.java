package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.entity.LawRevisionEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.domain.law.repository.LawRevisionRepository;
import com.aivle13.fin_audit_ai.domain.law.type.RevisionType;
import com.aivle13.fin_audit_ai.global.exception.llm.LlmServerErrorException;
import com.aivle13.fin_audit_ai.global.lawapi.client.LawApiClient;
import com.aivle13.fin_audit_ai.global.lawapi.client.LawArticleRevision;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LawRevisionApplierTest {

    private static final String LAW_NAME = "AI 기본법";
    private static final String OFFICIAL_LAW_NAME = "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법";

    @Mock
    private LawApiClient lawApiClient;

    @Mock
    private LawArticleRepository lawArticleRepository;

    @Mock
    private LawRevisionRepository lawRevisionRepository;

    @Mock
    private ReportLlmClient reportLlmClient;

    @InjectMocks
    private LawRevisionApplier lawRevisionApplier;

    @Test
    void appliesRevisionAndRegeneratesSummaryWhenApiDateIsNewer() {
        LawArticleEntity article = LawArticleEntity.of(
                LAW_NAME, "제31조", "옛날 조문 내용", LocalDate.of(2026, 1, 22)
        );
        article.updateSummary("옛날 요약");

        given(lawApiClient.fetchArticles(OFFICIAL_LAW_NAME)).willReturn(List.of(
                new LawArticleRevision("제31조", "새 조문 내용", LocalDate.of(2026, 7, 21))
        ));
        given(lawArticleRepository.findByLawNameAndArticleNo(LAW_NAME, "제31조"))
                .willReturn(Optional.of(article));
        given(reportLlmClient.generate(anyString(), eq("새 조문 내용")))
                .willReturn("새 요약");
        given(lawRevisionRepository.save(any(LawRevisionEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<LawRevisionEntity> revisions = lawRevisionApplier.applyForLaw(LAW_NAME, OFFICIAL_LAW_NAME);

        assertThat(article.getContent()).isEqualTo("새 조문 내용");
        assertThat(article.getRevisionDate()).isEqualTo(LocalDate.of(2026, 7, 21));
        assertThat(article.getSummary()).isEqualTo("새 요약");
        assertThat(article.getEmbedding()).isNull();

        assertThat(revisions).singleElement().satisfies(revision -> {
            assertThat(revision.getSource()).isEqualTo("law.go.kr");
            assertThat(revision.getRevisionType()).isEqualTo(RevisionType.AMENDMENT);
            assertThat(revision.getRevisedAt()).isEqualTo(LocalDate.of(2026, 7, 21));
        });
    }

    @Test
    void doesNothingWhenApiDateIsNotAfterLastKnownDate() {
        LawArticleEntity article = LawArticleEntity.of(LAW_NAME, "제6조", "원문", LocalDate.of(2026, 1, 22));

        given(lawApiClient.fetchArticles(OFFICIAL_LAW_NAME)).willReturn(List.of(
                new LawArticleRevision("제6조", "원문", LocalDate.of(2026, 1, 22))
        ));
        given(lawArticleRepository.findByLawNameAndArticleNo(LAW_NAME, "제6조"))
                .willReturn(Optional.of(article));

        List<LawRevisionEntity> revisions = lawRevisionApplier.applyForLaw(LAW_NAME, OFFICIAL_LAW_NAME);

        assertThat(revisions).isEmpty();
        verify(lawRevisionRepository, never()).save(any());
        verify(reportLlmClient, never()).generate(anyString(), anyString());
    }

    @Test
    void appliesRevisionWhenEffectiveDateIsSameButContentDiffers() {
        LawArticleEntity article = LawArticleEntity.of(LAW_NAME, "제6조", "원문", LocalDate.of(2026, 1, 22));

        given(lawApiClient.fetchArticles(OFFICIAL_LAW_NAME)).willReturn(List.of(
                new LawArticleRevision("제6조", "정정된 조문", LocalDate.of(2026, 1, 22))
        ));
        given(lawArticleRepository.findByLawNameAndArticleNo(LAW_NAME, "제6조"))
                .willReturn(Optional.of(article));
        given(reportLlmClient.generate(anyString(), eq("정정된 조문")))
                .willReturn("정정된 요약");
        given(lawRevisionRepository.save(any(LawRevisionEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<LawRevisionEntity> revisions = lawRevisionApplier.applyForLaw(LAW_NAME, OFFICIAL_LAW_NAME);

        assertThat(article.getContent()).isEqualTo("정정된 조문");
        assertThat(revisions).hasSize(1);
    }

    @Test
    void skipsArticleNotTrackedInLawArticles() {
        given(lawApiClient.fetchArticles(OFFICIAL_LAW_NAME)).willReturn(List.of(
                new LawArticleRevision("제99조", "내용", LocalDate.of(2026, 7, 21))
        ));
        given(lawArticleRepository.findByLawNameAndArticleNo(LAW_NAME, "제99조"))
                .willReturn(Optional.empty());

        List<LawRevisionEntity> revisions = lawRevisionApplier.applyForLaw(LAW_NAME, OFFICIAL_LAW_NAME);

        assertThat(revisions).isEmpty();
        verify(lawRevisionRepository, never()).save(any());
    }

    @Test
    void keepsOldSummaryWhenLlmRegenerationFails() {
        LawArticleEntity article = LawArticleEntity.of(LAW_NAME, "제31조", "옛 내용", LocalDate.of(2026, 1, 22));
        article.updateSummary("옛 요약");

        given(lawApiClient.fetchArticles(OFFICIAL_LAW_NAME)).willReturn(List.of(
                new LawArticleRevision("제31조", "새 내용", LocalDate.of(2026, 7, 21))
        ));
        given(lawArticleRepository.findByLawNameAndArticleNo(LAW_NAME, "제31조"))
                .willReturn(Optional.of(article));
        given(reportLlmClient.generate(anyString(), anyString()))
                .willThrow(new LlmServerErrorException());
        given(lawRevisionRepository.save(any(LawRevisionEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<LawRevisionEntity> revisions = lawRevisionApplier.applyForLaw(LAW_NAME, OFFICIAL_LAW_NAME);

        // 본문·시행일자는 이미 반영됐고, summary 재생성만 실패해 기존 값을 유지한다.
        assertThat(article.getContent()).isEqualTo("새 내용");
        assertThat(article.getSummary()).isEqualTo("옛 요약");
        assertThat(revisions).hasSize(1);
    }

    @Test
    void keepsOldSummaryAndSavesRevisionWhenLlmThrowsNonBusinessException() {
        LawArticleEntity article = LawArticleEntity.of(LAW_NAME, "제31조", "옛 내용", LocalDate.of(2026, 1, 22));
        article.updateSummary("옛 요약");

        given(lawApiClient.fetchArticles(OFFICIAL_LAW_NAME)).willReturn(List.of(
                new LawArticleRevision("제31조", "새 내용", LocalDate.of(2026, 7, 21))
        ));
        given(lawArticleRepository.findByLawNameAndArticleNo(LAW_NAME, "제31조"))
                .willReturn(Optional.of(article));
        given(reportLlmClient.generate(anyString(), anyString()))
                .willThrow(new IllegalStateException("연결이 갑자기 끊김"));
        given(lawRevisionRepository.save(any(LawRevisionEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // BusinessException 계열이 아닌 예외라도 조문 갱신·개정 저장은 계속 진행돼야 한다.
        List<LawRevisionEntity> revisions = lawRevisionApplier.applyForLaw(LAW_NAME, OFFICIAL_LAW_NAME);

        assertThat(article.getContent()).isEqualTo("새 내용");
        assertThat(article.getSummary()).isEqualTo("옛 요약");
        assertThat(revisions).hasSize(1);
    }
}
