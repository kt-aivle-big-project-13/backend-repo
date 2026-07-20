package com.aivle13.fin_audit_ai.global.exception.file;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class InvalidFileFormatException extends BusinessException {
    public InvalidFileFormatException() {
        super(ErrorCode.INVALID_FILE_FORMAT);
    }

    public InvalidFileFormatException(String message) {
        super(ErrorCode.INVALID_FILE_FORMAT, message);
    }
}
