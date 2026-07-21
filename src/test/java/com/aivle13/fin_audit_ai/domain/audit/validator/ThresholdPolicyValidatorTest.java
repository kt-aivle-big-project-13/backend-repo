package com.aivle13.fin_audit_ai.domain.audit.validator;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditUploadRequest;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidThresholdPolicyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThresholdPolicyValidatorTest {

    private final ThresholdPolicyValidator validator = new ThresholdPolicyValidator();

    private AuditUploadRequest requestWith(Double targetApprovalRate, Double threshold) {
        return new AuditUploadRequest(
                null, null, null,
                "audit", "model", null,
                targetApprovalRate, threshold,
                null
        );
    }

    @Test
    void 값이_모두_null이면_통과한다() {
        assertThatCode(() -> validator.validate(requestWith(null, null)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0})
    void 목표_승인율이_0에서_1_사이면_통과한다(double rate) {
        assertThatCode(() -> validator.validate(requestWith(rate, null)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 1.01, Double.NaN})
    void 목표_승인율이_범위를_벗어나면_예외가_발생한다(double rate) {
        AuditUploadRequest request = requestWith(rate, null);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(InvalidThresholdPolicyException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 0.5, 1.0})
    void 임계값이_0_초과_1_이하면_통과한다(double threshold) {
        assertThatCode(() -> validator.validate(requestWith(null, threshold)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.01, 1.01, Double.NaN})
    void 임계값이_범위를_벗어나면_예외가_발생한다(double threshold) {
        AuditUploadRequest request = requestWith(null, threshold);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(InvalidThresholdPolicyException.class);
    }
}
