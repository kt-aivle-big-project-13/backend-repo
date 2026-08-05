package com.aivle13.fin_audit_ai.global.exception.model.audit;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class DatasetAlreadyAuditedException extends BusinessException {
    public DatasetAlreadyAuditedException() {
        super(ErrorCode.DATASET_ALREADY_AUDITED);
    }
}
