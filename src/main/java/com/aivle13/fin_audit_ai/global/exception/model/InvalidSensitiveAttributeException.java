package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class InvalidSensitiveAttributeException extends BusinessException {
    public InvalidSensitiveAttributeException() {
        super(ErrorCode.INVALID_SENSITIVE_ATTRIBUTE);
    }

    public InvalidSensitiveAttributeException(String message) {
        super(ErrorCode.INVALID_SENSITIVE_ATTRIBUTE, message);
    }
}
