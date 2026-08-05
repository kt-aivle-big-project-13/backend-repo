package com.aivle13.fin_audit_ai.global.exception.model.audit;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class AuditAlreadyInProgressException extends BusinessException {
    public AuditAlreadyInProgressException() {
        super(ErrorCode.AUDIT_ALREADY_IN_PROGRESS);
    }
}
