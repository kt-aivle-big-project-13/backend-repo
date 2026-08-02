package com.aivle13.fin_audit_ai.global.exception.objection;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class DuplicateObjectionNoException extends BusinessException {
    public DuplicateObjectionNoException(String message) {
        super(ErrorCode.DUPLICATE_OBJECTION_NO, message);
    }
}