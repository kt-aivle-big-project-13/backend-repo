package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.selfcheck.SelfCheckAnswerSaveRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.selfcheck.SelfCheckAnswerResponse;
import com.aivle13.fin_audit_ai.domain.audit.service.selfcheck.SelfCheckAnswerService;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuditSelfCheckControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 21L;

    @Mock
    private SelfCheckAnswerService selfCheckAnswerService;

    @InjectMocks
    private AuditSelfCheckController controller;

    @Test
    void savesAnswersAndReturnsResponse() {
        SelfCheckAnswerSaveRequest request = new SelfCheckAnswerSaveRequest(List.of(
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.OVERSIGHT, true)
        ));
        SelfCheckAnswerResponse expected = new SelfCheckAnswerResponse(AUDIT_ID, List.of());

        given(selfCheckAnswerService.save(USER_ID, AUDIT_ID, request)).willReturn(expected);

        ResponseEntity<SelfCheckAnswerResponse> response = controller.save(USER_ID, AUDIT_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void returnsAnswers() {
        SelfCheckAnswerResponse expected = new SelfCheckAnswerResponse(AUDIT_ID, List.of());
        given(selfCheckAnswerService.get(USER_ID, AUDIT_ID)).willReturn(expected);

        ResponseEntity<SelfCheckAnswerResponse> response = controller.get(USER_ID, AUDIT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void throwsWhenSavingWithoutAuthentication() {
        SelfCheckAnswerSaveRequest request = new SelfCheckAnswerSaveRequest(List.of(
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.OVERSIGHT, true)
        ));

        assertThatThrownBy(() -> controller.save(null, AUDIT_ID, request))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(selfCheckAnswerService);
    }

    @Test
    void throwsWhenGettingWithoutAuthentication() {
        assertThatThrownBy(() -> controller.get(null, AUDIT_ID))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(selfCheckAnswerService);
    }
}
