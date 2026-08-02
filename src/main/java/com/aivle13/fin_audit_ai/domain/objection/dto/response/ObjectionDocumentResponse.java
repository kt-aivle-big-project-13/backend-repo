package com.aivle13.fin_audit_ai.domain.objection.dto.response;

import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;

public record ObjectionDocumentResponse(
        Long objectionId,
        String objectionNo,
        String customerName,
        ObjectionDecision decision,
        String explanation,
        String letterTitle,
        String letterBody
) {
}