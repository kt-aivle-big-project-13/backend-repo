package com.aivle13.fin_audit_ai.domain.report.dto;

// 설명가능성·편향·자가진단 결과를 공통 형식으로 표현
public record AuditMetricView(
        String metricName,
        String value,
        String status,
        String evidence
) {
}