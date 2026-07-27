package com.aivle13.fin_audit_ai.domain.report.template;

import java.util.List;

public final class ReportSectionTemplate {

    private static final List<String> SECTIONS = List.of(
            "1. 감사 개요",
            "2. 지표 요약",
            "3. 설명가능성 분석 결과",
            "4. 편향 진단 결과",
            "5. 조항별 규제 준수 결과",
            "6. 근거 서술",
            "7. 개선 권고 가이드"
    );

    private ReportSectionTemplate() {
    }

    public static List<String> sections() {
        return SECTIONS;
    }

    public static String asText() {
        return String.join("\n", SECTIONS);
    }
}