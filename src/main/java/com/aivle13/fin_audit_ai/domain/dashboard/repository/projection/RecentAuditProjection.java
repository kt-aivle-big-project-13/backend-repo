package com.aivle13.fin_audit_ai.domain.dashboard.repository.projection;

import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;

import java.time.LocalDateTime;

public interface RecentAuditProjection {

    Long getAuditId();

    Long getModelId();

    String getModelName();

    String getVersion();

    AuditStatus getStatus();

    LocalDateTime getCompletedAt();
}
