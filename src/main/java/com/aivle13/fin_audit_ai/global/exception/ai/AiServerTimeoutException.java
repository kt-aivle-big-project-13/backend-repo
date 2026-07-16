package com.aivle13.fin_audit_ai.global.exception.ai;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class AiServerTimeoutException extends BusinessException {
    public AiServerTimeoutException() {
        super(ErrorCode.AI_SERVER_TIMEOUT);
    }
}
