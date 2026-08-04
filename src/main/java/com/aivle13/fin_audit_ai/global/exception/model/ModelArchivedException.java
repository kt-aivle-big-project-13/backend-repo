package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class ModelArchivedException extends BusinessException {
    public ModelArchivedException() {
        super(ErrorCode.MODEL_ARCHIVED);
    }
}
