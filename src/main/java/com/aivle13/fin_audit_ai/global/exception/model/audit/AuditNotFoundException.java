package com.aivle13.fin_audit_ai.global.exception.model.audit;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class AuditNotFoundException extends BusinessException {

    public AuditNotFoundException() {
        super(ErrorCode.AUDIT_NOT_FOUND);
    }
}