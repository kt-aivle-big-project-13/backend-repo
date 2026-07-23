package com.aivle13.fin_audit_ai.global.exception.diagnosis;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class PreDiagnosisNotFoundException extends BusinessException {

    public PreDiagnosisNotFoundException() {
        super(ErrorCode.PRE_DIAGNOSIS_NOT_FOUND);
    }
}
