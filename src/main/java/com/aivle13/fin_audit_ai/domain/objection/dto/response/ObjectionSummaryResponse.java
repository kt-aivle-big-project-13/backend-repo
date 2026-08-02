package com.aivle13.fin_audit_ai.domain.objection.dto.response;

import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionStatus;

import java.time.LocalDateTime;

public record ObjectionSummaryResponse(
        Long objectionId,
        String objectionNo,
        String customerName,
        String title,
        String caseType,
        boolean highImpactAi,
        String status,
        LocalDateTime submittedAt
) {
    public static ObjectionSummaryResponse of(ObjectionEntity objection) {
        return new ObjectionSummaryResponse(
                objection.getId(),
                objection.getObjectionNo(),
                objection.getCustomerName(),
                objection.getTitle(),
                objection.getCaseType(),
                objection.getModel() != null && Boolean.TRUE.equals(objection.getModel().getHighImpact()),
                toApiStatus(objection.getStatus()),
                objection.getSubmittedAt()
        );
    }

    // ObjectionDetailResponse에서도 동일한 상태 매핑을 재사용한다.
    static String toApiStatus(ObjectionStatus status) {
        return status == ObjectionStatus.DELIVERED ? "COMPLETED" : "WAITING";
    }
}