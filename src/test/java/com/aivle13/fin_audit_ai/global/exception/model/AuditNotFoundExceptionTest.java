package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditNotFoundExceptionTest {

    @Test
    void carriesAuditNotFoundErrorCode() {
        AuditNotFoundException exception = new AuditNotFoundException();

        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUDIT_NOT_FOUND);
        assertThat(exception.getMessage())
                .isEqualTo(ErrorCode.AUDIT_NOT_FOUND.getMessage());
    }
}