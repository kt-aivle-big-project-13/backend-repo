package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class AuditNotCancellableException extends BusinessException {
    public AuditNotCancellableException() {
        super(ErrorCode.AUDIT_NOT_CANCELLABLE);
    }
}
