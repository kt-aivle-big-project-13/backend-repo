package com.aivle13.fin_audit_ai.domain.dashboard.repository.projection;

import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;

public interface FairnessMetricCountProjection {

    FairnessMetricCode getMetricCode();

    FairnessStatus getStatus();

    Long getCount();
}
