package com.aivle13.fin_audit_ai.global.ai.client.report;

import com.aivle13.fin_audit_ai.global.ai.dto.report.request.ExplainabilityReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ExplainabilityReportResponse;

public interface ExplainabilityReportClient {

    ExplainabilityReportResponse generate(
            ExplainabilityReportRequest request
    );
}
