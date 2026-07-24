package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditProgressServiceTest {

    private static final Long AUDIT_ID = 1L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private FairnessResultRepository fairnessResultRepository;

    @Mock
    private XaiResultRepository xaiResultRepository;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private AuditProgressService auditProgressService;

    private FairnessResultEntity fairness(FairnessStatus status) {
        FairnessResultEntity result = org.mockito.Mockito.mock(FairnessResultEntity.class);
        org.mockito.Mockito.lenient().when(result.getStatus()).thenReturn(status);
        return result;
    }

    private XaiResultEntity xai(XaiStatus status) {
        XaiResultEntity result = org.mockito.Mockito.mock(XaiResultEntity.class);
        org.mockito.Mockito.lenient().when(result.getStatus()).thenReturn(status);
        return result;
    }

    @Test
    void marksAuditAsInProgress() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        auditProgressService.markInProgress(AUDIT_ID);

        verify(audit).markInProgress();
    }

    @Test
    void movesAuditToFairnessStepWhenShapIsCompleted() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        auditProgressService.markShapCompleted(AUDIT_ID);

        verify(audit).moveToStep(3);
    }

    @Test
    void completesAsCompliantWhenAllMetricsPass() {
        List<FairnessResultEntity> fairnessResults = List.of(fairness(FairnessStatus.PASS));
        List<XaiResultEntity> xaiResults = List.of(xai(XaiStatus.PASS));
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));
        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(fairnessResults);
        given(xaiResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(xaiResults);

        auditProgressService.markFairnessCompleted(AUDIT_ID);

        verify(audit).complete(4, AuditStatus.COMPLIANT);
    }

    @Test
    void completesAsNonCompliantWhenAnyFairnessFails() {
        List<FairnessResultEntity> fairnessResults =
                List.of(fairness(FairnessStatus.PASS), fairness(FairnessStatus.FAIL));
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));
        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(fairnessResults);
        given(xaiResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of());

        auditProgressService.markFairnessCompleted(AUDIT_ID);

        verify(audit).complete(4, AuditStatus.NON_COMPLIANT);
    }

    @Test
    void completesAsWarningWhenXaiReviewButNoFail() {
        // 공정성은 모두 PASS 지만 XAI 에 REVIEW 가 있어 WARNING 으로 종합
        List<FairnessResultEntity> fairnessResults = List.of(fairness(FairnessStatus.PASS));
        List<XaiResultEntity> xaiResults = List.of(xai(XaiStatus.REVIEW));
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));
        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(fairnessResults);
        given(xaiResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(xaiResults);

        auditProgressService.markFairnessCompleted(AUDIT_ID);

        verify(audit).complete(4, AuditStatus.WARNING);
    }

    @Test
    void marksAuditAsFailed() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        auditProgressService.markFailed(AUDIT_ID);

        verify(audit).markFailed();
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                auditProgressService.markInProgress(AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);
    }
}