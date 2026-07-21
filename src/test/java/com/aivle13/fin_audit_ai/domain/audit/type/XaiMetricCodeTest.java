package com.aivle13.fin_audit_ai.domain.audit.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XaiMetricCodeTest {

    @Test
    void containsExpectedConstants() {
        assertThat(XaiMetricCode.values())
                .containsExactlyInAnyOrder(
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        XaiMetricCode.CONSISTENCY,
                        XaiMetricCode.GLOBAL_STABILITY,
                        XaiMetricCode.FIDELITY
                );
    }

    @Test
    void resolvesGlobalStabilityByName() {
        assertThat(XaiMetricCode.valueOf("GLOBAL_STABILITY"))
                .isEqualTo(XaiMetricCode.GLOBAL_STABILITY);
    }
}