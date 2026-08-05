package com.aivle13.fin_audit_ai.domain.dashboard.repository.projection;

import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;

public interface AuditStatusCountProjection {

    AuditStatus getStatus();

    Long getCount();
}
