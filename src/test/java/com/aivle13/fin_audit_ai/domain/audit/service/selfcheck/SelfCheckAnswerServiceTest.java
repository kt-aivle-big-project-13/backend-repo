package com.aivle13.fin_audit_ai.domain.audit.service.selfcheck;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.selfcheck.SelfCheckAnswerSaveRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.selfcheck.SelfCheckAnswerResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidSelfCheckAnswersException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SelfCheckAnswerServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 21L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private SelfCheckAnswerRepository selfCheckAnswerRepository;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private SelfCheckAnswerService selfCheckAnswerService;

    @Test
    void savesAllFiveAnswersAndReturnsResponse() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        SelfCheckAnswerSaveRequest request = new SelfCheckAnswerSaveRequest(List.of(
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.PRIOR_NOTICE, true),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.SUPERVISION, true),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.EXPLANATION_PROCEDURE, false),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.RISK_MANAGEMENT, true),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.DOC_RETENTION, false)
        ));

        given(selfCheckAnswerRepository.saveAll(anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));

        SelfCheckAnswerResponse response = selfCheckAnswerService.save(USER_ID, AUDIT_ID, request);

        verify(selfCheckAnswerRepository).deleteAllByAudit_Id(AUDIT_ID);

        ArgumentCaptor<List<SelfCheckAnswerEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(selfCheckAnswerRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(5);

        assertThat(response.auditId()).isEqualTo(AUDIT_ID);
        assertThat(response.answers()).hasSize(5);
    }

    @Test
    void throwsWhenAnswerIsMissingAnItem() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        SelfCheckAnswerSaveRequest request = new SelfCheckAnswerSaveRequest(List.of(
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.PRIOR_NOTICE, true),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.SUPERVISION, true),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.EXPLANATION_PROCEDURE, false),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.RISK_MANAGEMENT, true)
        ));

        assertThatThrownBy(() -> selfCheckAnswerService.save(USER_ID, AUDIT_ID, request))
                .isInstanceOf(InvalidSelfCheckAnswersException.class);

        verify(selfCheckAnswerRepository, never()).deleteAllByAudit_Id(AUDIT_ID);
        verify(selfCheckAnswerRepository, never()).saveAll(anyList());
    }

    @Test
    void throwsWhenAnswerHasDuplicateItem() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        SelfCheckAnswerSaveRequest request = new SelfCheckAnswerSaveRequest(List.of(
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.PRIOR_NOTICE, true),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.SUPERVISION, true),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.SUPERVISION, false),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.RISK_MANAGEMENT, true),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.DOC_RETENTION, false)
        ));

        assertThatThrownBy(() -> selfCheckAnswerService.save(USER_ID, AUDIT_ID, request))
                .isInstanceOf(InvalidSelfCheckAnswersException.class);

        verify(selfCheckAnswerRepository, never()).deleteAllByAudit_Id(AUDIT_ID);
    }

    @Test
    void throwsWhenAuditNotOwnedByUser() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> selfCheckAnswerService.get(USER_ID, AUDIT_ID))
                .isInstanceOf(AuditNotFoundException.class);

        verify(selfCheckAnswerRepository, never()).findAllByAudit_Id(AUDIT_ID);
    }

    @Test
    void returnsEmptyAnswersWhenNoneSubmittedYet() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(selfCheckAnswerRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of());

        SelfCheckAnswerResponse response = selfCheckAnswerService.get(USER_ID, AUDIT_ID);

        assertThat(response.auditId()).isEqualTo(AUDIT_ID);
        assertThat(response.answers()).isEmpty();
    }
}
