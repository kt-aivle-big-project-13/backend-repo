package com.aivle13.fin_audit_ai.global.exception.mail;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class EmailSendFailedException extends BusinessException {
    public EmailSendFailedException() {
        super(ErrorCode.EMAIL_SEND_FAILED);
    }

    public EmailSendFailedException(Throwable cause) {
        super(ErrorCode.EMAIL_SEND_FAILED, cause);
    }
}
