package com.aivle13.fin_audit_ai.global.ai.client.report;

import com.aivle13.fin_audit_ai.global.ai.dto.report.request.ImprovementGuideRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ImprovementGuideResponse;

public interface ImprovementGuideClient {

    ImprovementGuideResponse generate(
            ImprovementGuideRequest request
    );
}
