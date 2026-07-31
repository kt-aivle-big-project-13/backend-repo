package com.aivle13.fin_audit_ai.domain.dashboard.repository.projection;

import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;

public interface ReviewRequiredModelProjection {

    Long getModelId();

    String getModelName();

    Long getIssueCount();

    AuditStatus getStatus();
}
