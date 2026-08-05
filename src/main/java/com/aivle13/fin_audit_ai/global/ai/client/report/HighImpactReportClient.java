package com.aivle13.fin_audit_ai.global.ai.client.report;

import com.aivle13.fin_audit_ai.global.ai.dto.report.request.HighImpactReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.HighImpactReportResponse;

public interface HighImpactReportClient {

    HighImpactReportResponse generate(
            HighImpactReportRequest request
    );
}
