package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class SensitiveAttributesNotSelectedException extends BusinessException {
    public SensitiveAttributesNotSelectedException() {
        super(ErrorCode.SENSITIVE_ATTRIBUTES_NOT_SELECTED);
    }
}
