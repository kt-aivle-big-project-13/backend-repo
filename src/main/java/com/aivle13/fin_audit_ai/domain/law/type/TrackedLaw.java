package com.aivle13.fin_audit_ai.domain.law.type;

import lombok.Getter;

/**
 * 시딩·개정 감지 대상 법령. law_articles.law_name(축약형)·law.go.kr 정식 법령명·시딩 CSV
 * 경로가 한 세트로 묶여야 하는데, 예전엔 {@code LawArticleSeeder}와
 * {@code LawRevisionDetectionService}가 이 세트를 각자 따로 들고 있어 한쪽만 갱신하고
 * 다른 쪽을 놓치기 쉬웠다.
 */
@Getter
public enum TrackedLaw {

    AI_BASIC_ACT(
            "AI 기본법",
            "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법",
            "db/seed/ai-basic-act.csv"
    ),
    AI_BASIC_ACT_DECREE(
            "AI 기본법 시행령",
            "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법 시행령",
            "db/seed/ai-basic-act-decree.csv"
    );

    private final String lawName;
    private final String officialName;
    private final String seedFile;

    TrackedLaw(String lawName, String officialName, String seedFile) {
        this.lawName = lawName;
        this.officialName = officialName;
        this.seedFile = seedFile;
    }
}
