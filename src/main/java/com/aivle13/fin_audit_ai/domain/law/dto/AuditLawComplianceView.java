package com.aivle13.fin_audit_ai.domain.law.dto;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditLawMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;

// 보고서 생성에 사용할 법령 매핑 조회 결과
public record AuditLawComplianceView(
        Long mappingId,
        String articleNumber,
        String articleTitle,
        String content,
        ComplianceStatus compliance,
        String evidence
) {
    public static AuditLawComplianceView from(AuditLawMappingEntity mapping) {
        return new AuditLawComplianceView(
                mapping.getId(),
                mapping.getArticle().getArticleNo(),
                mapping.getArticle().getLawName(),
                mapping.getArticle().getContent(),
                mapping.getCompliance(),
                mapping.getEvidence()
        );
    }
}