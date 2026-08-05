package com.aivle13.fin_audit_ai.global.exception.model.dataset;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class FileSizeExceededException extends BusinessException {
    public FileSizeExceededException() {
        super(ErrorCode.FILE_SIZE_EXCEEDED);
    }

    public FileSizeExceededException(String message) {
        super(ErrorCode.FILE_SIZE_EXCEEDED, message);
    }
}
