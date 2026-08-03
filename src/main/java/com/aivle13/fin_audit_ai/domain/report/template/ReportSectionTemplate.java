package com.aivle13.fin_audit_ai.domain.report.template;

import java.util.List;
import java.util.stream.Collectors;

public final class ReportSectionTemplate {

    private static final List<String> SECTIONS = List.of(
            "1. 감사 개요",
            "2. 종합 요약",
            "3. 설명가능성 분석 결과",
            "4. 편향 진단 결과",
            "5. 규제 준수 판정",
            "6. 개선 권고 가이드"
    );

    private ReportSectionTemplate() {
    }

    public static List<String> sections() {
        return SECTIONS;
    }

    /**
     * 프롬프트에 넣을 섹션 구조.
     *
     * <p>제목 문법(`##`)을 붙여 그대로 따라 쓰게 한다. 규칙만 글로 설명하면 LLM 이 다른 문법을
     * 섞어 쓰고, 그 기호가 PDF·Word 에 그대로 인쇄된다.
     */
    public static String asText() {
        return SECTIONS.stream()
                .map(section -> "## " + section)
                .collect(Collectors.joining("\n"));
    }
}