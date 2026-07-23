package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class FairnessResultNotFoundException extends BusinessException {

    public FairnessResultNotFoundException() {
        super(ErrorCode.FAIRNESS_RESULT_NOT_FOUND);
    }
}
