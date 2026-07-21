package com.aivle13.fin_audit_ai.domain.audit.dto;

import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class XaiMetricResponseDtoTest {

    @Test
    void mapsAllFieldsFromEntity() {
        BigDecimal value = new BigDecimal("0.9996");
        BigDecimal threshold = new BigDecimal("0.7000");

        XaiResultEntity entity = XaiResultEntity.of(
                null,
                XaiMetricCode.GLOBAL_STABILITY,
                value,
                threshold,
                XaiStatus.PASS
        );

        XaiMetricResponseDto dto = XaiMetricResponseDto.from(entity);

        assertThat(dto.metricCode()).isEqualTo(XaiMetricCode.GLOBAL_STABILITY);
        assertThat(dto.value()).isEqualTo(value);
        assertThat(dto.threshold()).isEqualTo(threshold);
        assertThat(dto.status()).isEqualTo(XaiStatus.PASS);
    }
}