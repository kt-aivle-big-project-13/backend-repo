package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.HighImpactReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.HighImpactReportResponse;

public interface HighImpactReportClient {

    HighImpactReportResponse generate(
            HighImpactReportRequest request
    );
}
