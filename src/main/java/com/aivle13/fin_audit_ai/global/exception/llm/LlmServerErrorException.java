package com.aivle13.fin_audit_ai.global.exception.llm;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class LlmServerErrorException extends BusinessException {

    public LlmServerErrorException() {
        super(ErrorCode.EXTERNAL_API_ERROR);
    }

    public LlmServerErrorException(String message) {
        super(ErrorCode.EXTERNAL_API_ERROR, message);
    }

    public LlmServerErrorException(Throwable cause) {
        super(ErrorCode.EXTERNAL_API_ERROR, cause);
    }
}