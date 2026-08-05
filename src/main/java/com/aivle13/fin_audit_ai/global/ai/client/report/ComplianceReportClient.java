package com.aivle13.fin_audit_ai.global.ai.client.report;

import com.aivle13.fin_audit_ai.global.ai.dto.report.request.ComplianceReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ComplianceReportResponse;

public interface ComplianceReportClient {

    ComplianceReportResponse generate(
            ComplianceReportRequest request
    );
}
