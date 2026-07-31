package com.aivle13.fin_audit_ai.domain.law.dto.response;

import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.law.dto.MatchedChecklistItem;

import java.util.List;

public record RegulationMappingResponse(
        Long auditId,
        List<RegulationMapping> mappings
) {
    public static RegulationMappingResponse of(Long auditId, List<AuditRegulationComplianceView> views) {
        List<RegulationMapping> mappings = views.stream()
                .map(RegulationMapping::from)
                .toList();

        return new RegulationMappingResponse(auditId, mappings);
    }

    public record RegulationMapping(
            Long mappingId,
            String regulation,
            String article,
            String content,
            String summary,
            ComplianceStatus compliance,
            String evidence,
            List<MatchedChecklistItem> matchedItems
    ) {
        public static RegulationMapping from(AuditRegulationComplianceView view) {
            return new RegulationMapping(
                    view.mappingId(),
                    view.articleTitle(),
                    view.articleNumber(),
                    view.content(),
                    view.summary(),
                    view.compliance(),
                    view.evidence(),
                    view.matchedItems()
            );
        }
    }
}
