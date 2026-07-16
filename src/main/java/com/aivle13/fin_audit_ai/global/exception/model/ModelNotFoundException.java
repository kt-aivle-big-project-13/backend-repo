package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class ModelNotFoundException extends BusinessException {
    public ModelNotFoundException() {
        super(ErrorCode.MODEL_NOT_FOUND);
    }
}
