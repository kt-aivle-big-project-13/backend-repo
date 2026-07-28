package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessMetricResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessResultResponse;
import com.aivle13.fin_audit_ai.domain.audit.service.fairness.FairnessResultService;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditFairnessControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 21L;

    @Mock
    private FairnessResultService fairnessResultService;

    @InjectMocks
    private AuditFairnessController controller;

    @Test
    void returnsFairnessResult() {
        FairnessResultResponse expected =
                new FairnessResultResponse(
                        AUDIT_ID,
                        "FAIRLEARN",
                        List.of(
                                new FairnessMetricResponse(
                                        "CODE_GENDER",
                                        FairnessMetricCode.EQUALIZED_ODDS,
                                        new BigDecimal("0.1230"),
                                        new BigDecimal("0.1000"),
                                        FairnessStatus.FAIL,
                                        null
                                )
                        )
                );

        given(fairnessResultService.getFairness(USER_ID, AUDIT_ID, null))
                .willReturn(expected);

        ResponseEntity<FairnessResultResponse> response =
                controller.getFairness(USER_ID, AUDIT_ID, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void passesAttributeFilterToService() {
        FairnessResultResponse expected =
                new FairnessResultResponse(AUDIT_ID, "FAIRLEARN", List.of());

        given(fairnessResultService.getFairness(USER_ID, AUDIT_ID, "CODE_GENDER"))
                .willReturn(expected);

        ResponseEntity<FairnessResultResponse> response =
                controller.getFairness(USER_ID, AUDIT_ID, "CODE_GENDER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(fairnessResultService).getFairness(USER_ID, AUDIT_ID, "CODE_GENDER");
    }

    @Test
    void throwsWhenUserIsNotAuthenticated() {
        assertThatThrownBy(() ->
                controller.getFairness(null, AUDIT_ID, null)
        ).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(fairnessResultService);
    }

    @Test
    void doesNotExposeFairnessResultSaveEndpoint() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        mockMvc.perform(
                        post("/api/v1/audits/{auditId}/fairness", AUDIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(fairnessResultService);
    }
}