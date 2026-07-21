package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class InvalidModelFileException extends BusinessException {
    public InvalidModelFileException() {
        super(ErrorCode.INVALID_MODEL_FILE);
    }

    public InvalidModelFileException(String message) {
        super(ErrorCode.INVALID_MODEL_FILE, message);
    }
}
