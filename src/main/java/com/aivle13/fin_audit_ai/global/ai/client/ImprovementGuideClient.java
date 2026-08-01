package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.ai.dto.ImprovementGuideRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ImprovementGuideResponse;

public interface ImprovementGuideClient {

    ImprovementGuideResponse generate(
            ImprovementGuideRequest request
    );
}
