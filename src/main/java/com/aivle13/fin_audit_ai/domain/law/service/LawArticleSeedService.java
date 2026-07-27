package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import lombok.RequiredArgsConstructor;
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

/**
 * CSV 한 개 분량을 하나의 트랜잭션으로 시딩한다.
 * (law_name, article_no) 기준으로 이미 있는 조항은 건너뛰므로, 실패 후 재실행해도
 * 이전에 성공한 조항을 중복 삽입하지 않고 빠진 조항만 다시 채운다.
 */
@Service
@RequiredArgsConstructor
public class LawArticleSeedService {

    private final LawArticleRepository lawArticleRepository;

    @Transactional(rollbackFor = Exception.class)
    public int seedFrom(String classpathPath) throws IOException {
        String csv = readAsUtf8WithoutBom(classpathPath);
        int inserted = 0;

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(new StringReader(csv))) {
            for (CSVRecord record : parser) {
                String lawName = record.get("law_name");
                String articleNo = record.get("article_no");

                if (lawArticleRepository.existsByLawNameAndArticleNo(lawName, articleNo)) {
                    continue;
                }

                LawArticleEntity entity = LawArticleEntity.of(
                        lawName,
                        articleNo,
                        record.get("content"),
                        LocalDate.parse(record.get("effective_date"))
                );

                String summary = record.get("summary");
                if (summary != null && !summary.isBlank()) {
                    entity.updateSummary(summary);
                }

                lawArticleRepository.saveAndFlush(entity);
                inserted++;
            }
        }

        return inserted;
    }

    private String readAsUtf8WithoutBom(String classpathPath) throws IOException {
        try (InputStream in = new ClassPathResource(classpathPath).getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return content.startsWith("﻿") ? content.substring(1) : content;
        }
    }
}
