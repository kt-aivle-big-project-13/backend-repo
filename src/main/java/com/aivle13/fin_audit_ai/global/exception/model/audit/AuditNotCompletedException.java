package com.aivle13.fin_audit_ai.global.exception.model.audit;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class AuditNotCompletedException extends BusinessException {

    public AuditNotCompletedException() {
        super(ErrorCode.AUDIT_NOT_COMPLETED);
    }
}