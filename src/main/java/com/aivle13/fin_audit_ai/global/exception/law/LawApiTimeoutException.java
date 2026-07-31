package com.aivle13.fin_audit_ai.global.exception.law;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class LawApiTimeoutException extends BusinessException {
    public LawApiTimeoutException() {
        super(ErrorCode.LAW_API_TIMEOUT);
    }
}
