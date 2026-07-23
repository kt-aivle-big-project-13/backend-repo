package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.FairnessRunRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.FairnessRunResponse;

public interface FairnessAnalysisClient {

    FairnessRunResponse analyze(FairnessRunRequest request);
}
