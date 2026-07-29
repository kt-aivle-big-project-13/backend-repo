package com.aivle13.fin_audit_ai.domain.report.dto;

import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;

import java.util.List;

// 최종 보고서 생성에 필요한 모든 입력 데이터
public record ReportGenerationContext(
        Long auditId,
        List<AuditMetricView> xaiResults,
        List<AuditMetricView> fairnessResults,
        List<AuditMetricView> selfCheckResults,
        List<AuditRegulationComplianceView> regulationCompliances,
        List<String> improvementGuides
) {
}