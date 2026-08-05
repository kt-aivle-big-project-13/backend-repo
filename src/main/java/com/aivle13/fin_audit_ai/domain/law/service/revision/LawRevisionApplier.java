package com.aivle13.fin_audit_ai.domain.law.service.revision;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.entity.LawRevisionEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.domain.law.repository.LawRevisionRepository;
import com.aivle13.fin_audit_ai.domain.law.type.RevisionType;
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
import java.util.Optional;

/**
 * 법령 하나에 대해 실제로 API를 조회하고 개정을 반영하는 트랜잭션 단위.
 * {@link LawRevisionDetectionService}에서 이 클래스를 별도 빈으로 주입받아 호출해야
 * {@code @Transactional}이 프록시를 거쳐 실제로 적용된다 — 같은 클래스 안에서
 * this로 자기 자신의 @Transactional 메서드를 호출하면 프록시를 우회해 트랜잭션이
 * 걸리지 않고, article이 조회 직후 detached 상태가 돼 필드 변경이 저장 안 되는
 * 문제가 있었다(code review로 발견).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LawRevisionApplier {

    private static final String SUMMARY_SYSTEM_PROMPT = """
            너는 금융 AI 거버넌스 감사 플랫폼에서 법령 조항을 요약하는 역할이다.
            주어진 조문 원문을 핵심 의무·대상·근거 위주로 한두 문장의 간결한 한국어로 요약하라.
            불필요한 수식어 없이 사실만 전달하라.
            """;

    private final LawApiClient lawApiClient;
    private final LawArticleRepository lawArticleRepository;
    private final LawRevisionRepository lawRevisionRepository;
    private final ReportLlmClient reportLlmClient;

    @Transactional
    public List<LawRevisionEntity> applyForLaw(String lawName, String officialLawName) {
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

        boolean isNewerRevision = apiArticle.effectiveDate().isAfter(lastKnownDate);
        boolean isSameDateContentFix = apiArticle.effectiveDate().isEqual(lastKnownDate)
                && !apiArticle.content().equals(article.getContent());

        // 전부개정처럼 내용이 안 바뀐 조문까지 시행일자가 갱신되는 경우가 있어, 시행일자가
        // 최신이라는 것만으로는 개정으로 볼 수 없다. law.go.kr이 내려주는 조문변경여부로
        // 실제로 이 조문의 내용이 바뀐 게 맞는지 한 번 더 확인한다.
        boolean isActuallyRevised = (isNewerRevision && apiArticle.changed()) || isSameDateContentFix;

        if (!isActuallyRevised) {
            if (isNewerRevision) {
                article.acknowledgeEffectiveDate(apiArticle.effectiveDate());
            }
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
        } catch (RuntimeException exception) {
            log.warn(
                    "조문 요약 재생성 실패, 기존 summary를 유지함: lawName={}, articleNo={}",
                    article.getLawName(),
                    article.getArticleNo(),
                    exception
            );
        }
    }
}
