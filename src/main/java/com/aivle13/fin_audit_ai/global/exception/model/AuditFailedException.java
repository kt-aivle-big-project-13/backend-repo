package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class AuditFailedException extends BusinessException {
    public AuditFailedException() {
        super(ErrorCode.AUDIT_FAILED);
    }

    public AuditFailedException(Throwable cause) {
        super(ErrorCode.AUDIT_FAILED, cause);
    }
}
