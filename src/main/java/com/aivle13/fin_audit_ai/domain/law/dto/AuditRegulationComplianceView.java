package com.aivle13.fin_audit_ai.domain.law.dto;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditRegulationMappingEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;

import java.util.List;

// 보고서 생성에 사용할 법령 매핑 조회 결과
public record AuditRegulationComplianceView(
        Long mappingId,
        String articleNumber,
        String articleTitle,
        String content,
        String summary,
        ComplianceStatus compliance,
        String evidence,
        List<SelfCheckItemCode> itemCodes,
        String paragraphLabel
) {
    // itemCodes는 DB에 저장된 값이 아니라, 자율점검 답변을 ARTICLE_MAPPING에 대입해
    // 조회 시점에 역산한 값이다(AuditRegulationMappingService.resolveItemCodesByArticle).
    // paragraphLabel은 조 전체가 아니라 특정 항만 해당할 때만 채워지는 표시용 값으로,
    // 없으면 null이다(AuditRegulationMappingService.PARAGRAPH_LABELS).
    public static AuditRegulationComplianceView from(
            AuditRegulationMappingEntity mapping,
            List<SelfCheckItemCode> itemCodes,
            String paragraphLabel
    ) {
        return new AuditRegulationComplianceView(
                mapping.getId(),
                mapping.getArticle().getArticleNo(),
                mapping.getArticle().getLawName(),
                mapping.getArticle().getContent(),
                mapping.getArticle().getSummary(),
                mapping.getCompliance(),
                mapping.getEvidence(),
                itemCodes,
                paragraphLabel
        );
    }
}
