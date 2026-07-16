package com.aivle13.fin_audit_ai.global.exception.ai;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class AiServerErrorException extends BusinessException {
    public AiServerErrorException() {
        super(ErrorCode.AI_SERVER_ERROR);
    }

    public AiServerErrorException(Throwable cause) {
        super(ErrorCode.AI_SERVER_ERROR, cause);
    }
}
