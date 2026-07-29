package com.aivle13.fin_audit_ai.domain.law.controller;

import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.law.dto.AuditRegulationComplianceView;
import com.aivle13.fin_audit_ai.domain.law.dto.response.RegulationMappingResponse;
import com.aivle13.fin_audit_ai.domain.law.service.AuditRegulationMappingService;
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
class AuditRegulationMappingControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 21L;

    @Mock
    private AuditRegulationMappingService auditRegulationMappingService;

    @InjectMocks
    private AuditRegulationMappingController controller;

    @Test
    void returnsRegulationMappings() {
        AuditRegulationComplianceView view = new AuditRegulationComplianceView(
                1L, "제12조", "인공지능 기본법", "조문 원문", ComplianceStatus.NON_COMPLIANT, "근거"
        );
        given(auditRegulationMappingService.getMappings(USER_ID, AUDIT_ID)).willReturn(List.of(view));

        ResponseEntity<RegulationMappingResponse> response =
                controller.getRegulationMappings(USER_ID, AUDIT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(
                RegulationMappingResponse.of(AUDIT_ID, List.of(view))
        );
    }

    @Test
    void throwsWhenUserIsNotAuthenticated() {
        assertThatThrownBy(() ->
                controller.getRegulationMappings(null, AUDIT_ID)
        ).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(auditRegulationMappingService);
    }
}
