package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditNotCompletedExceptionTest {

    @Test
    void carriesAuditNotCompletedErrorCode() {
        AuditNotCompletedException exception = new AuditNotCompletedException();

        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUDIT_NOT_COMPLETED);
        assertThat(exception.getMessage())
                .isEqualTo(ErrorCode.AUDIT_NOT_COMPLETED.getMessage());
    }
}