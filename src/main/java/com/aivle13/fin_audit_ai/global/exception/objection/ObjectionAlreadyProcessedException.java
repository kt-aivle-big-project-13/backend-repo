package com.aivle13.fin_audit_ai.global.exception.objection;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class ObjectionAlreadyProcessedException extends BusinessException {
    public ObjectionAlreadyProcessedException() {
        super(ErrorCode.OBJECTION_ALREADY_PROCESSED);
    }
}