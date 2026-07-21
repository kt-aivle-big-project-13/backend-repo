package com.aivle13.fin_audit_ai.global.exception.file;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class EmptyFileException extends BusinessException {
    public EmptyFileException() {
        super(ErrorCode.EMPTY_FILE);
    }

    public EmptyFileException(String message) {
        super(ErrorCode.EMPTY_FILE, message);
    }
}
