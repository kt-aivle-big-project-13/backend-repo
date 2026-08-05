package com.aivle13.fin_audit_ai.global.exception.model.explainability;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class ExplainabilityResultNotFoundException extends BusinessException {

    public ExplainabilityResultNotFoundException() {
        super(ErrorCode.EXPLAINABILITY_RESULT_NOT_FOUND);
    }
}