package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class ValidationDatasetNotFoundException extends BusinessException {
    public ValidationDatasetNotFoundException() {
        super(ErrorCode.VALIDATION_DATASET_NOT_FOUND);
    }
}
