package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.BiasReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.BiasReportResponse;

public interface BiasReportClient {

    BiasReportResponse generate(
            BiasReportRequest request
    );
}
