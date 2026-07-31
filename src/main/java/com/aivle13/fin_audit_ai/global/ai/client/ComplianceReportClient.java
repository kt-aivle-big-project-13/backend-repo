package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.ComplianceReportRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ComplianceReportResponse;

public interface ComplianceReportClient {

    ComplianceReportResponse generate(
            ComplianceReportRequest request
    );
}
