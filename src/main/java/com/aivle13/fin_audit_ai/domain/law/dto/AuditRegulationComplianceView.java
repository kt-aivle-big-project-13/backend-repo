package com.aivle13.fin_audit_ai.domain.law.dto;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditRegulationMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;

// 보고서 생성에 사용할 법령 매핑 조회 결과
public record AuditRegulationComplianceView(
        Long mappingId,
        String articleNumber,
        String articleTitle,
        ComplianceStatus compliance,
        String evidence
) {
    public static AuditRegulationComplianceView from(AuditRegulationMappingEntity mapping) {
        return new AuditRegulationComplianceView(
                mapping.getId(),
                mapping.getArticle().getArticleNo(),
                mapping.getArticle().getLawName(),
                mapping.getCompliance(),
                mapping.getEvidence()
        );
    }
}
