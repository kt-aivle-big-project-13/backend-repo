package com.aivle13.fin_audit_ai.global.exception.objection;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class InvalidObjectionFileException extends BusinessException {
    public InvalidObjectionFileException(String message) {
        super(ErrorCode.INVALID_OBJECTION_FILE, message);
    }
}