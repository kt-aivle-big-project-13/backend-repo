package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import com.aivle13.fin_audit_ai.domain.law.repository.LawArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 로컬 개발 환경에서 법령 조항 원문(엑셀 기반 CSV)을 law_articles 테이블에 시딩한다.
 * dev는 ddl-auto=create라 매 기동 시 테이블이 새로 만들어지므로, 기동할 때마다 다시 채워 넣는다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class LawArticleSeeder implements ApplicationRunner {

    private static final List<String> SEED_FILES = List.of(
            "db/seed/ai-basic-act.csv",
            "db/seed/ai-basic-act-decree.csv"
    );

    private final LawArticleRepository lawArticleRepository;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (lawArticleRepository.count() > 0) {
            return;
        }

        for (String seedFile : SEED_FILES) {
            int count = seedFrom(seedFile);
            log.info("법령 조항 시딩 완료: {} ({}건)", seedFile, count);
        }
    }

    private int seedFrom(String classpathPath) throws IOException {
        String csv = readAsUtf8WithoutBom(classpathPath);
        int count = 0;

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(new StringReader(csv))) {
            for (CSVRecord record : parser) {
                LawArticleEntity entity = LawArticleEntity.of(
                        record.get("law_name"),
                        record.get("article_no"),
                        record.get("content"),
                        LocalDate.parse(record.get("effective_date"))
                );

                String summary = record.get("summary");
                if (summary != null && !summary.isBlank()) {
                    entity.updateSummary(summary);
                }

                lawArticleRepository.save(entity);
                count++;
            }
        }

        return count;
    }

    private String readAsUtf8WithoutBom(String classpathPath) throws IOException {
        try (InputStream in = new ClassPathResource(classpathPath).getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return content.startsWith("﻿") ? content.substring(1) : content;
        }
    }
}
