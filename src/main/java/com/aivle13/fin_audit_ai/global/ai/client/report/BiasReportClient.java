package com.aivle13.fin_audit_ai.global.ai.client.report;

import com.aivle13.fin_audit_ai.global.ai.dto.report.request.BiasReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.BiasReportResponse;

public interface BiasReportClient {

    BiasReportResponse generate(
            BiasReportRequest request
    );
}
