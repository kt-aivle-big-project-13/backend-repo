package com.aivle13.fin_audit_ai.global.exception.model;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainabilityResultNotFoundExceptionTest {

    @Test
    void carriesExplainabilityResultNotFoundErrorCode() {
        ExplainabilityResultNotFoundException exception =
                new ExplainabilityResultNotFoundException();

        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.EXPLAINABILITY_RESULT_NOT_FOUND);
        assertThat(exception.getMessage())
                .isEqualTo(ErrorCode.EXPLAINABILITY_RESULT_NOT_FOUND.getMessage());
    }
}