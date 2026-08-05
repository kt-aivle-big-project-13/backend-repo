package com.aivle13.fin_audit_ai.domain.audit.service.selfcheck;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.selfcheck.SelfCheckAnswerSaveRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.selfcheck.SelfCheckAnswerResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckAnswerValue;
import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.domain.law.service.mapping.AuditRegulationMappingService;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.selfcheck.InvalidSelfCheckAnswersException;
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
    private AuditRegulationMappingService auditRegulationMappingService;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private SelfCheckAnswerService selfCheckAnswerService;

    @Test
    void savesSubmittedAnswersAndReturnsResponse() {
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        SelfCheckAnswerSaveRequest request = new SelfCheckAnswerSaveRequest(List.of(
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.TR_01, SelfCheckAnswerValue.YES),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.HO_01, SelfCheckAnswerValue.YES),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.UP_01, SelfCheckAnswerValue.NO),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.RM_01, SelfCheckAnswerValue.YES),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.DC_01, SelfCheckAnswerValue.NO)
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

        verify(auditRegulationMappingService).mapFromSelfCheckAnswers(AUDIT_ID);
    }

    // 21문항 중 일부만 제출해도 정상 저장된다 — 미응답 항목을 강제로 채우지 않는다.
    @Test
    void savesPartialAnswersSuccessfully() {
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        SelfCheckAnswerSaveRequest request = new SelfCheckAnswerSaveRequest(List.of(
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.TR_01, SelfCheckAnswerValue.YES)
        ));

        given(selfCheckAnswerRepository.saveAll(anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));

        SelfCheckAnswerResponse response = selfCheckAnswerService.save(USER_ID, AUDIT_ID, request);

        verify(selfCheckAnswerRepository).deleteAllByAudit_Id(AUDIT_ID);
        assertThat(response.answers()).hasSize(1);
        verify(auditRegulationMappingService).mapFromSelfCheckAnswers(AUDIT_ID);
    }

    @Test
    void throwsWhenAnswerHasDuplicateItem() {
        given(auditRepository.findByIdAndUser_IdForUpdate(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        SelfCheckAnswerSaveRequest request = new SelfCheckAnswerSaveRequest(List.of(
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.TR_01, SelfCheckAnswerValue.YES),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.HO_01, SelfCheckAnswerValue.YES),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.HO_01, SelfCheckAnswerValue.NO),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.RM_01, SelfCheckAnswerValue.YES),
                new SelfCheckAnswerSaveRequest.Item(SelfCheckItemCode.DC_01, SelfCheckAnswerValue.NO)
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
