package com.aivle13.fin_audit_ai.global.exception.objection;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class ObjectionNotFoundException extends BusinessException {
    public ObjectionNotFoundException() {
        super(ErrorCode.OBJECTION_NOT_FOUND);
    }
}