package com.aivle13.fin_audit_ai.global.exception.llm;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class LlmServerTimeoutException extends BusinessException {

    public LlmServerTimeoutException(Throwable cause) {
        super(ErrorCode.AI_SERVER_TIMEOUT, cause);
    }
}