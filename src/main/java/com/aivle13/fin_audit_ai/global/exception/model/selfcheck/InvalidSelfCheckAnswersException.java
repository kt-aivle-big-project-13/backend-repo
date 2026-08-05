package com.aivle13.fin_audit_ai.global.exception.model.selfcheck;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class InvalidSelfCheckAnswersException extends BusinessException {
    public InvalidSelfCheckAnswersException() {
        super(ErrorCode.INVALID_SELF_CHECK_ANSWERS);
    }
}
