package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class DuplicateModelNameException extends BusinessException {
    public DuplicateModelNameException() {
        super(ErrorCode.DUPLICATE_MODEL_NAME);
    }
}