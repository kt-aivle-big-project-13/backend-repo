package com.aivle13.fin_audit_ai.global.lawapi.client;

import com.aivle13.fin_audit_ai.global.exception.law.LawApiErrorException;
import com.aivle13.fin_audit_ai.global.exception.law.LawApiTimeoutException;
import com.aivle13.fin_audit_ai.global.lawapi.config.LawApiProperties;
import com.aivle13.fin_audit_ai.global.lawapi.dto.LawGoKrTextFlattener;
import com.aivle13.fin_audit_ai.global.lawapi.dto.LawServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * law.go.kr 법령 본문 조회(target=law, type=JSON). 인증 실패 등 API 자체 오류는 HTTP
 * 200으로 {"result":..,"msg":..} 형태만 내려오고 "법령" 키가 없으므로, 역직렬화된
 * law가 null이면 오류로 간주한다.
 */
@Slf4j
@Component
public class LawGoKrApiClient implements LawApiClient {

    private static final String LAW_SERVICE_PATH = "/DRF/lawService.do";
    private static final DateTimeFormatter EFFECTIVE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CHANGED_FLAG_VALUE = "Y";

    private final RestClient restClient;
    private final LawApiProperties properties;

    public LawGoKrApiClient(
            @Qualifier("lawApiRestClient") RestClient restClient,
            LawApiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public List<LawArticleRevision> fetchArticles(String officialLawName) {
        try {
            LawServiceResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(LAW_SERVICE_PATH)
                            .queryParam("OC", properties.ocKey())
                            .queryParam("target", "law")
                            .queryParam("LM", officialLawName)
                            .queryParam("type", "JSON")
                            .build())
                    .retrieve()
                    .body(LawServiceResponse.class);

            if (response == null || response.law() == null || response.law().articles() == null) {
                throw new LawApiErrorException();
            }

            List<LawServiceResponse.ArticleUnit> units = response.law().articles().units();
            if (units == null) {
                return List.of();
            }

            return units.stream()
                    .filter(LawServiceResponse.ArticleUnit::isActualArticle)
                    .map(this::toRevision)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (ResourceAccessException exception) {
            if (hasTimeoutCause(exception)) {
                throw new LawApiTimeoutException();
            }

            throw new LawApiErrorException(exception);
        } catch (RestClientException exception) {
            throw new LawApiErrorException(exception);
        }
    }

    private LawArticleRevision toRevision(LawServiceResponse.ArticleUnit unit) {
        String articleNo = normalizeArticleNo(unit.articleNo(), unit.articleSubNo());

        if (unit.effectiveDate() == null || unit.effectiveDate().isBlank()) {
            log.warn("시행일자가 없는 조문이라 건너뜀: articleNo={}", articleNo);
            return null;
        }

        String content = LawGoKrTextFlattener.flatten(unit.content(), unit.paragraphs());
        LocalDate effectiveDate = LocalDate.parse(unit.effectiveDate(), EFFECTIVE_DATE_FORMAT);
        boolean changed = CHANGED_FLAG_VALUE.equals(unit.changed());

        return new LawArticleRevision(articleNo, content, effectiveDate, changed);
    }

    private String normalizeArticleNo(String articleNo, String articleSubNo) {
        String base = "제" + articleNo + "조";

        if (articleSubNo == null || articleSubNo.isBlank() || "0".equals(articleSubNo)) {
            return base;
        }

        return base + "의" + articleSubNo;
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable cause = throwable;

        while (cause != null) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
