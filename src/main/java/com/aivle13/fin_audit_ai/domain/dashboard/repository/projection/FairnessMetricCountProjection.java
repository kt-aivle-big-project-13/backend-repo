package com.aivle13.fin_audit_ai.domain.dashboard.repository.projection;

import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus;

public interface FairnessMetricCountProjection {

    FairnessMetricCode getMetricCode();

    FairnessStatus getStatus();

    Long getCount();
}
