package com.aivle13.fin_audit_ai.global.exception.model.dataset;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class DatasetNotFoundException extends BusinessException {
    public DatasetNotFoundException() {
        super(ErrorCode.DATASET_NOT_FOUND);
    }
}
