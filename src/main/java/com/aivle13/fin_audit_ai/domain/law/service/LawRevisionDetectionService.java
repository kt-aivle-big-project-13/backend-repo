package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.entity.LawRevisionEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.domain.law.repository.LawRevisionRepository;
import com.aivle13.fin_audit_ai.domain.law.type.RevisionType;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.lawapi.client.LawApiClient;
import com.aivle13.fin_audit_ai.global.lawapi.client.LawArticleRevision;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * law.go.kr에서 추적 대상 법령의 현재 조문을 조회해, 우리 DB에 저장된 조문보다 시행일자가
 * 최신이면 개정으로 간주하고 반영한다. 법령·조문 단위로 실패를 격리해 하나가 실패해도
 * 나머지는 계속 처리한다 — {@link LawArticleEmbeddingService#embedMissingArticles()}와
 * 동일한 lenient 배치 패턴이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LawRevisionDetectionService {

    // law_articles.law_name(축약형) → law.go.kr 정식 법령명
    private static final Map<String, String> TRACKED_LAWS = Map.of(
            "AI 기본법", "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법",
            "AI 기본법 시행령", "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법 시행령"
    );

    private static final String SUMMARY_SYSTEM_PROMPT = """
            너는 금융 AI 거버넌스 감사 플랫폼에서 법령 조항을 요약하는 역할이다.
            주어진 조문 원문을 핵심 의무·대상·근거 위주로 한두 문장의 간결한 한국어로 요약하라.
            불필요한 수식어 없이 사실만 전달하라.
            """;

    private final LawApiClient lawApiClient;
    private final LawArticleRepository lawArticleRepository;
    private final LawRevisionRepository lawRevisionRepository;
    private final ReportLlmClient reportLlmClient;

    public List<LawRevisionEntity> detectAndApply() {
        List<LawRevisionEntity> revisions = new ArrayList<>();

        for (Map.Entry<String, String> trackedLaw : TRACKED_LAWS.entrySet()) {
            try {
                revisions.addAll(detectAndApplyForLaw(trackedLaw.getKey(), trackedLaw.getValue()));
            } catch (RuntimeException exception) {
                log.error(
                        "법령 개정 감지 실패, 다음 추적 대상 법령으로 계속함: lawName={}",
                        trackedLaw.getKey(),
                        exception
                );
            }
        }

        return revisions;
    }

    @Transactional
    public List<LawRevisionEntity> detectAndApplyForLaw(String lawName, String officialLawName) {
        List<LawArticleRevision> apiArticles = lawApiClient.fetchArticles(officialLawName);
        List<LawRevisionEntity> revisions = new ArrayList<>();

        for (LawArticleRevision apiArticle : apiArticles) {
            try {
                applyIfRevised(lawName, apiArticle).ifPresent(revisions::add);
            } catch (RuntimeException exception) {
                log.error(
                        "조문 개정 반영 실패, 다음 조문으로 계속함: lawName={}, articleNo={}",
                        lawName,
                        apiArticle.articleNo(),
                        exception
                );
            }
        }

        return revisions;
    }

    private Optional<LawRevisionEntity> applyIfRevised(String lawName, LawArticleRevision apiArticle) {
        LawArticleEntity article = lawArticleRepository
                .findByLawNameAndArticleNo(lawName, apiArticle.articleNo())
                .orElse(null);

        if (article == null) {
            log.warn(
                    "정적으로 추적하지 않는 조문이라 건너뜀: lawName={}, articleNo={}",
                    lawName,
                    apiArticle.articleNo()
            );
            return Optional.empty();
        }

        LocalDate lastKnownDate = article.getRevisionDate() != null
                ? article.getRevisionDate()
                : article.getEffectiveDate();

        if (!apiArticle.effectiveDate().isAfter(lastKnownDate)) {
            return Optional.empty();
        }

        article.applyRevision(apiArticle.content(), apiArticle.effectiveDate());
        regenerateSummary(article);

        LawRevisionEntity revision = LawRevisionEntity.of(
                "law.go.kr",
                lawName + " " + apiArticle.articleNo() + " 개정",
                RevisionType.AMENDMENT,
                apiArticle.effectiveDate(),
                LocalDateTime.now()
        );

        return Optional.of(lawRevisionRepository.save(revision));
    }

    // summary는 content 변경 시점에 자동으로 재작성할 사람이 없으므로 LLM으로 대신 재요약한다.
    // 실패해도 조문 갱신 자체는 이미 반영됐으니 배치를 막지 않고, 낡은 summary를 유지한 채
    // 경고만 남긴다 — 다음 실행에서 다시 시도되진 않지만(embedding만 재시도 대상), 사람이
    // 로그를 보고 수동으로 재요약할 수 있다.
    private void regenerateSummary(LawArticleEntity article) {
        try {
            String summary = reportLlmClient.generate(SUMMARY_SYSTEM_PROMPT, article.getContent());
            article.updateSummary(summary);
        } catch (BusinessException exception) {
            log.warn(
                    "조문 요약 재생성 실패, 기존 summary를 유지함: lawName={}, articleNo={}",
                    article.getLawName(),
                    article.getArticleNo(),
                    exception
            );
        }
    }
}
