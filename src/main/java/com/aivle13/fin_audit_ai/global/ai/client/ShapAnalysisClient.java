package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.explainability.ExplainabilityResultRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.ShapAnalysisRequest;

public interface ShapAnalysisClient {

    ExplainabilityResultRequest analyze(
            ShapAnalysisRequest request
    );
}