package com.aivle13.fin_audit_ai.domain.audit.dto.response.core;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;

import java.time.LocalDateTime;

/**
 * 감사 목록 항목.
 *
 * <p>{@code assessmentId} 는 이 감사에 연결된 고영향 AI 사전진단이다. 사전진단은 건너뛸 수
 * 있어({@code AuditStartService#linkDiagnosis}) 값이 없을 수 있고, 그때는 사전진단 보고서를
 * 만들 수 없다. 화면에서 해당 보고서를 아예 내보내지 않으려면 이 값이 필요하다.
 */
public record AuditSummaryResponse(
        Long auditId,
        String modelName,
        String modelFileName,
        String datasetFileName,
        String modelGroupId,
        String version,
        Long assessmentId,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        AuditStatus status,
        int currentStep
) {
    public static AuditSummaryResponse from(AuditEntity audit) {
        return new AuditSummaryResponse(
                audit.getId(),
                audit.getModel().getModelName(),
                audit.getModel().getOriginalFileName(),
                audit.getDataset().getOriginalFileName(),
                audit.getModel().getModelGroupId(),
                audit.getModel().getVersion(),
                audit.getAssessmentId(),
                audit.getCreatedAt(),
                audit.getCompletedAt(),
                audit.getStatus(),
                audit.getCurrentStep()
        );
    }
}
