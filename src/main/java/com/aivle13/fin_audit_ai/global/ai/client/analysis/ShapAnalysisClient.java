package com.aivle13.fin_audit_ai.global.ai.client.analysis;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.explainability.ExplainabilityResultRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.analysis.request.ShapAnalysisRequest;

public interface ShapAnalysisClient {

    ExplainabilityResultRequest analyze(
            ShapAnalysisRequest request
    );
}