package com.aivle13.fin_audit_ai.domain.law.service.seed;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import com.aivle13.fin_audit_ai.global.lawapi.client.LawApiClient;
import com.aivle13.fin_audit_ai.global.lawapi.client.LawArticleRevision;
import com.aivle13.fin_audit_ai.global.lawapi.config.LawApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CSV 한 개 분량을 하나의 트랜잭션으로 시딩한다.
 * (law_name, article_no) 기준으로 이미 있는 조항은 건너뛰므로, 실패 후 재실행해도
 * 이전에 성공한 조항을 중복 삽입하지 않고 빠진 조항만 다시 채운다.
 *
 * <p>content는 가능하면 CSV의 손질된 텍스트 대신 law.go.kr API가 내려주는 조문을
 * {@code LawGoKrTextFlattener}로 평탄화한 텍스트를 그대로 저장한다. 시딩 시점 content가
 * {@link com.aivle13.fin_audit_ai.domain.law.service.revision.LawRevisionApplier}가
 * 개정 감지 때 비교하는 API content와 애초에 같은 포맷이어야, 실제로는 안 바뀐 조문이
 * 포맷 차이만으로 개정 오탐되는 일이 없다. API를 못 쓰면(OC 키 미발급, 장애) CSV
 * content로 폴백해 로컬 개발 흐름은 그대로 유지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LawArticleSeedService {

    private final LawArticleRepository lawArticleRepository;
    private final LawApiClient lawApiClient;
    private final LawApiProperties lawApiProperties;

    @Transactional(rollbackFor = Exception.class)
    public int seedFrom(String classpathPath, String officialLawName) throws IOException {
        List<CSVRecord> missingRecords = readMissingRecords(classpathPath);

        if (missingRecords.isEmpty()) {
            return 0;
        }

        // 시딩할 신규 조항이 있을 때만 law.go.kr을 호출한다. 이미 다 채워진 상태로 재기동해도
        // (existsByLawNameAndArticleNo가 매번 걸러내므로) 매번 API를 두드리는 낭비가 없도록.
        Map<String, LawArticleRevision> apiArticlesByNo = fetchApiArticles(officialLawName);
        int inserted = 0;

        for (CSVRecord record : missingRecords) {
            String lawName = record.get("law_name");
            String articleNo = record.get("article_no");

            LawArticleRevision apiArticle = apiArticlesByNo.get(articleNo);
            String content = apiArticle != null ? apiArticle.content() : record.get("content");
            LocalDate effectiveDate = apiArticle != null
                    ? apiArticle.effectiveDate()
                    : LocalDate.parse(record.get("effective_date"));

            LawArticleEntity entity = LawArticleEntity.of(lawName, articleNo, content, effectiveDate);

            String summary = record.get("summary");
            if (summary != null && !summary.isBlank()) {
                entity.updateSummary(summary);
            }

            lawArticleRepository.saveAndFlush(entity);
            inserted++;
        }

        return inserted;
    }

    private List<CSVRecord> readMissingRecords(String classpathPath) throws IOException {
        String csv = readAsUtf8WithoutBom(classpathPath);
        List<CSVRecord> missingRecords = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(new StringReader(csv))) {
            for (CSVRecord record : parser) {
                if (!lawArticleRepository.existsByLawNameAndArticleNo(record.get("law_name"), record.get("article_no"))) {
                    missingRecords.add(record);
                }
            }
        }

        return missingRecords;
    }

    private Map<String, LawArticleRevision> fetchApiArticles(String officialLawName) {
        if (!lawApiProperties.enabled()) {
            log.warn("법령 API 비활성화로 시딩 content를 CSV 원문으로 대체함: officialLawName={}", officialLawName);
            return Map.of();
        }

        try {
            return lawApiClient.fetchArticles(officialLawName).stream()
                    .collect(Collectors.toMap(LawArticleRevision::articleNo, Function.identity()));
        } catch (RuntimeException exception) {
            log.warn(
                    "법령 API 조회 실패로 시딩 content를 CSV 원문으로 대체함: officialLawName={}",
                    officialLawName,
                    exception
            );
            return Map.of();
        }
    }

    private String readAsUtf8WithoutBom(String classpathPath) throws IOException {
        try (InputStream in = new ClassPathResource(classpathPath).getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return content.startsWith("﻿") ? content.substring(1) : content;
        }
    }
}
