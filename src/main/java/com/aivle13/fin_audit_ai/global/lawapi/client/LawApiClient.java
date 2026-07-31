package com.aivle13.fin_audit_ai.global.lawapi.client;

import java.util.List;

public interface LawApiClient {

    /**
     * 정식 법령명으로 현행 법령 본문을 조회해 조문 목록을 반환한다.
     *
     * @param officialLawName law.go.kr 기준 정식 법령명 (예: "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법")
     */
    List<LawArticleRevision> fetchArticles(String officialLawName);
}
