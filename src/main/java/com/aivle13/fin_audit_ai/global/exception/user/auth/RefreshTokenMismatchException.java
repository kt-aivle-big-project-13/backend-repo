package com.aivle13.fin_audit_ai.global.exception.user.auth;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class RefreshTokenMismatchException extends BusinessException {
    public RefreshTokenMismatchException() {
        super(ErrorCode.REFRESH_TOKEN_MISMATCH);
    }
}