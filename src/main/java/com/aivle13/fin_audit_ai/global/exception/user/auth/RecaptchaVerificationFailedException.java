package com.aivle13.fin_audit_ai.global.exception.user.auth;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class RecaptchaVerificationFailedException extends BusinessException {

    public RecaptchaVerificationFailedException() {
        super(ErrorCode.RECAPTCHA_VERIFICATION_FAILED);
    }
}