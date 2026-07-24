package com.aivle13.fin_audit_ai.global.exception.external;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class RecaptchaServerException extends BusinessException {

    public RecaptchaServerException() {
        super(ErrorCode.RECAPTCHA_SERVER_ERROR);
    }
}