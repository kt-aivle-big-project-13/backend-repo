package com.aivle13.fin_audit_ai.domain.report.prompt;

import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.report.dto.AuditMetricView;
import com.aivle13.fin_audit_ai.domain.report.dto.ReportGenerationContext;
import com.aivle13.fin_audit_ai.domain.report.template.ReportSectionTemplate;

import java.util.List;

public final class ReportPromptBuilder {

    private ReportPromptBuilder() {
    }

    // 감사 결과 데이터를 LLM 입력 프롬프트로 조립
    public static String build(ReportGenerationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("보고서 생성 컨텍스트는 필수입니다.");
        }

        StringBuilder prompt = new StringBuilder();

        prompt.append("[보고서 작성 요청]\n")
                .append("아래 감사 원본 수치와 확정된 법령 준수 결과만 사용하여 ")
                .append("최종 감사 보고서를 작성하세요.\n\n");

        prompt.append("[감사 ID]\n")
                .append(context.auditId())
                .append("\n\n");

        appendMetrics(
                prompt,
                "설명가능성 분석 결과",
                context.xaiResults()
        );

        appendMetrics(
                prompt,
                "편향 진단 결과",
                context.fairnessResults()
        );

        appendMetrics(
                prompt,
                "자가진단 결과",
                context.selfCheckResults()
        );

        appendRegulationCompliances(
                prompt,
                context.regulationCompliances()
        );

        appendImprovementGuides(
                prompt,
                context.improvementGuides()
        );

        prompt.append("\n[출력 지침]\n")
                .append(ReportSectionTemplate.asText());

        return prompt.toString();
    }

    // 감사 지표 목록을 프롬프트 형식으로 변환
    private static void appendMetrics(
            StringBuilder prompt,
            String title,
            List<AuditMetricView> metrics
    ) {
        prompt.append('[')
                .append(title)
                .append("]\n");

        if (metrics == null || metrics.isEmpty()) {
            prompt.append("- 조회된 결과 없음\n\n");
            return;
        }

        for (AuditMetricView metric : metrics) {
            prompt.append("- 지표명: ")
                    .append(valueOrEmpty(metric.metricName()))
                    .append('\n')
                    .append("  값: ")
                    .append(valueOrEmpty(metric.value()))
                    .append('\n')
                    .append("  상태: ")
                    .append(valueOrEmpty(metric.status()))
                    .append('\n')
                    .append("  근거: ")
                    .append(valueOrEmpty(metric.evidence()))
                    .append('\n');
        }

        prompt.append('\n');
    }

    // 확정된 법령 준수 결과를 프롬프트 형식으로 변환
    private static void appendRegulationCompliances(
            StringBuilder prompt,
            List<AuditRegulationComplianceView> mappings
    ) {
        prompt.append("[확정된 법령 준수 결과]\n");

        if (mappings == null || mappings.isEmpty()) {
            prompt.append("- 확정된 법령 매핑 없음\n\n");
            return;
        }

        for (AuditRegulationComplianceView mapping : mappings) {
            prompt.append("- 조항: ")
                    .append(valueOrEmpty(mapping.articleNumber()))
                    .append(' ')
                    .append(valueOrEmpty(mapping.articleTitle()))
                    .append('\n')
                    .append("  준수 상태: ")
                    .append(mapping.compliance())
                    .append('\n')
                    .append("  근거: ")
                    .append(valueOrEmpty(mapping.evidence()))
                    .append('\n');
        }

        prompt.append('\n');
    }

    // 확정된 개선 권고 내용을 프롬프트 형식으로 변환
    private static void appendImprovementGuides(
            StringBuilder prompt,
            List<String> guides
    ) {
        prompt.append("[개선 권고]\n");

        if (guides == null || guides.isEmpty()) {
            prompt.append("- 확정된 개선 권고 없음\n");
            return;
        }

        for (String guide : guides) {
            prompt.append("- ")
                    .append(valueOrEmpty(guide))
                    .append('\n');
        }
    }

    // null 값을 프롬프트에 그대로 노출하지 않도록 변환
    private static String valueOrEmpty(String value) {
        return value == null || value.isBlank()
                ? "없음"
                : value;
    }
}