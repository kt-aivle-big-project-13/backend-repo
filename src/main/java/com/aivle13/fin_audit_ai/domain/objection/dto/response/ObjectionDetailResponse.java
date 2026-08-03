package com.aivle13.fin_audit_ai.domain.objection.dto.response;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.List;

public record ObjectionDetailResponse(
        Long objectionId,
        String objectionNo,
        String customerName,
        String title,
        String content,
        String caseType,
        boolean highImpactAi,
        Long modelId,
        String modelName,
        String shapEvidence,
        String staffNote,
        List<ObjectionModelEvidenceResponse> globalModelEvidence,
        String status,
        ObjectionDecision decision,
        String draftContent,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime deliveredAt,
        String approverName,
        String recipientEmail
) {
    public static ObjectionDetailResponse of(
            ObjectionEntity objection
    ) {
        return of(objection, List.of());
    }

    public static ObjectionDetailResponse of(
            ObjectionEntity objection,
            List<ObjectionModelEvidenceResponse> globalModelEvidence
    ) {
        AiModelEntity model = objection.getModel();
        UserEntity approver = objection.getApprover();

        return new ObjectionDetailResponse(
                objection.getId(),
                objection.getObjectionNo(),
                objection.getCustomerName(),
                objection.getTitle(),
                objection.getContent(),
                objection.getCaseType(),
                model != null
                        && Boolean.TRUE.equals(model.getHighImpact()),
                model != null ? model.getId() : null,
                model != null ? model.getModelName() : null,
                objection.getShapEvidence(),
                objection.getStaffNote(),
                List.copyOf(globalModelEvidence),
                ObjectionSummaryResponse.toApiStatus(
                        objection.getStatus()
                ),
                objection.getDecision(),
                objection.getDraftContent(),
                objection.getSubmittedAt(),
                objection.getApprovedAt(),
                objection.getDeliveredAt(),
                approver != null ? approver.getName() : null,
                objection.getRecipientEmail()
        );
    }
}