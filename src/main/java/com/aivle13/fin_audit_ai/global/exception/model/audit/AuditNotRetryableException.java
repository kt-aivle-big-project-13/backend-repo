package com.aivle13.fin_audit_ai.global.exception.model.audit;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class AuditNotRetryableException extends BusinessException {
    public AuditNotRetryableException() {
        super(ErrorCode.AUDIT_NOT_RETRYABLE);
    }
}
