package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.ExplainabilityReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ExplainabilityReportResponse;

public interface ExplainabilityReportClient {

    ExplainabilityReportResponse generate(
            ExplainabilityReportRequest request
    );
}
