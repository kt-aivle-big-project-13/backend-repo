package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.explainability.ExplainabilityResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.explainability.XaiMetricResponse;
import com.aivle13.fin_audit_ai.domain.audit.service.explainability.ExplainabilityService;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus;
import com.aivle13.fin_audit_ai.global.exception.user.auth.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        ExplainabilityResponse expected =
                new ExplainabilityResponse(
                        AUDIT_ID,
                        "SHAP",
                        List.of(
                                new XaiMetricResponse(
                                        XaiMetricCode.FIDELITY,
                                        new BigDecimal("0.4843"),
                                        new BigDecimal("0.5000"),
                                        XaiStatus.REVIEW
                                )
                        ),
                        List.of()
                );

        given(explainabilityService.getExplainability(USER_ID, AUDIT_ID))
                .willReturn(expected);

        ResponseEntity<ExplainabilityResponse> response =
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
    void doesNotExposeExplainabilityResultSaveEndpoint() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        mockMvc.perform(
                        post("/api/v1/audits/{auditId}/explainability", AUDIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(explainabilityService);
    }
}