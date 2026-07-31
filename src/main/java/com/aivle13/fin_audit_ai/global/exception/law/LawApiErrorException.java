package com.aivle13.fin_audit_ai.global.exception.law;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class LawApiErrorException extends BusinessException {
    public LawApiErrorException() {
        super(ErrorCode.LAW_API_ERROR);
    }

    public LawApiErrorException(Throwable cause) {
        super(ErrorCode.LAW_API_ERROR, cause);
    }
}
