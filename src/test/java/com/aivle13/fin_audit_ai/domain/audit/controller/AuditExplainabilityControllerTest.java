package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.ExplainabilityResponseDto;
import com.aivle13.fin_audit_ai.domain.audit.dto.XaiMetricResponseDto;
import com.aivle13.fin_audit_ai.domain.audit.service.ExplainabilityService;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuditExplainabilityControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 21L;

    @Mock
    private ExplainabilityService explainabilityService;

    @InjectMocks
    private AuditExplainabilityController controller;

    @Test
    void returnsExplainabilityResult() {
        ExplainabilityResponseDto expected =
                new ExplainabilityResponseDto(
                        AUDIT_ID,
                        "SHAP",
                        List.of(
                                new XaiMetricResponseDto(
                                        XaiMetricCode.FIDELITY,
                                        new BigDecimal("0.4843"),
                                        new BigDecimal("0.5000"),
                                        XaiStatus.REVIEW
                                )
                        )
                );

        given(explainabilityService.getExplainability(USER_ID, AUDIT_ID))
                .willReturn(expected);

        ResponseEntity<ExplainabilityResponseDto> response =
                controller.getExplainability(USER_ID, AUDIT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void throwsWhenUserIsNotAuthenticated() {
        assertThatThrownBy(() ->
                controller.getExplainability(null, AUDIT_ID)
        ).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(explainabilityService);
    }

    @Test
    void delegatesToServiceWithGivenUserAndAuditId() {
        ExplainabilityResponseDto expected =
                new ExplainabilityResponseDto(AUDIT_ID, "SHAP", List.of());

        given(explainabilityService.getExplainability(USER_ID, AUDIT_ID))
                .willReturn(expected);

        controller.getExplainability(USER_ID, AUDIT_ID);

        verify(explainabilityService).getExplainability(USER_ID, AUDIT_ID);
    }

    @Test
    void propagatesExceptionThrownByService() {
        given(explainabilityService.getExplainability(USER_ID, AUDIT_ID))
                .willThrow(new AuditNotFoundException());

        assertThatThrownBy(() ->
                controller.getExplainability(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);
    }
}