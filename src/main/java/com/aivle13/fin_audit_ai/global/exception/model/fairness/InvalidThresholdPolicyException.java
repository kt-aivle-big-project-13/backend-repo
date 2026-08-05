package com.aivle13.fin_audit_ai.global.exception.model.fairness;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class InvalidThresholdPolicyException extends BusinessException {
    public InvalidThresholdPolicyException() {
        super(ErrorCode.INVALID_THRESHOLD_POLICY);
    }

    public InvalidThresholdPolicyException(String message) {
        super(ErrorCode.INVALID_THRESHOLD_POLICY, message);
    }
}
