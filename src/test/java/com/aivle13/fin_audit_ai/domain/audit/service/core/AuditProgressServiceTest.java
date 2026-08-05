package com.aivle13.fin_audit_ai.domain.audit.service.core;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.fairness.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.explainability.XaiStatus;
import com.aivle13.fin_audit_ai.domain.notification.service.NotificationService;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditProgressServiceTest {

    private static final Long AUDIT_ID = 1L;
    private static final int GENERATION = 0;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private FairnessResultRepository fairnessResultRepository;

    @Mock
    private XaiResultRepository xaiResultRepository;

    @Mock
    private NotificationService notificationService;

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
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));

        auditProgressService.markInProgress(AUDIT_ID, GENERATION);

        verify(audit).markInProgress();
    }

    @Test
    void doesNotMarkInProgressWhenAlreadyCancelled() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.isCancelled()).willReturn(true);

        auditProgressService.markInProgress(AUDIT_ID, GENERATION);

        verify(audit, never()).markInProgress();
    }

    @Test
    void doesNotMarkInProgressWhenGenerationIsStale() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.getGeneration()).willReturn(1);

        auditProgressService.markInProgress(AUDIT_ID, GENERATION);

        verify(audit, never()).markInProgress();
    }

    @Test
    void movesAuditToFairnessStepWhenShapIsCompleted() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));

        auditProgressService.markShapCompleted(AUDIT_ID, GENERATION);

        verify(audit).moveToStep(3);
    }

    @Test
    void doesNotMoveToFairnessStepWhenCancelled() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.isCancelled()).willReturn(true);

        auditProgressService.markShapCompleted(AUDIT_ID, GENERATION);

        verify(audit, never()).moveToStep(anyInt());
    }

    @Test
    void doesNotMoveToFairnessStepWhenGenerationIsStale() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.getGeneration()).willReturn(1);

        auditProgressService.markShapCompleted(AUDIT_ID, GENERATION);

        verify(audit, never()).moveToStep(anyInt());
    }

    @Test
    void completesAsCompliantWhenAllMetricsPass() {
        List<FairnessResultEntity> fairnessResults = List.of(fairness(FairnessStatus.PASS));
        List<XaiResultEntity> xaiResults = List.of(xai(XaiStatus.PASS));
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));
        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(fairnessResults);
        given(xaiResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(xaiResults);

        auditProgressService.markFairnessCompleted(AUDIT_ID, GENERATION);

        verify(audit).complete(4, AuditStatus.COMPLIANT);
        verify(notificationService).notifyAuditComplete(audit);
        verify(notificationService, never()).notifyReauditRecommend(audit);
    }

    @Test
    void completesAsNonCompliantWhenAnyFairnessFails() {
        List<FairnessResultEntity> fairnessResults =
                List.of(fairness(FairnessStatus.PASS), fairness(FairnessStatus.FAIL));
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));
        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(fairnessResults);
        given(xaiResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(List.of());

        auditProgressService.markFairnessCompleted(AUDIT_ID, GENERATION);

        verify(audit).complete(4, AuditStatus.NON_COMPLIANT);
        verify(notificationService).notifyReauditRecommend(audit);
    }

    @Test
    void completesAsWarningWhenXaiReviewButNoFail() {
        // 공정성은 모두 PASS 지만 XAI 에 REVIEW 가 있어 WARNING 으로 종합
        List<FairnessResultEntity> fairnessResults = List.of(fairness(FairnessStatus.PASS));
        List<XaiResultEntity> xaiResults = List.of(xai(XaiStatus.REVIEW));
        given(auditRepository.findByIdForUpdate(AUDIT_ID)).willReturn(Optional.of(audit));
        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(fairnessResults);
        given(xaiResultRepository.findAllByAudit_Id(AUDIT_ID)).willReturn(xaiResults);

        auditProgressService.markFairnessCompleted(AUDIT_ID, GENERATION);

        verify(audit).complete(4, AuditStatus.WARNING);
        verify(notificationService).notifyReauditRecommend(audit);
    }

    @Test
    void doesNotCompleteWhenCancelled() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.isCancelled()).willReturn(true);

        auditProgressService.markFairnessCompleted(AUDIT_ID, GENERATION);

        verify(audit, never()).complete(anyInt(), any());
        verify(notificationService, never()).notifyAuditComplete(audit);
    }

    @Test
    void doesNotCompleteWhenGenerationIsStale() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.getGeneration()).willReturn(1);

        auditProgressService.markFairnessCompleted(AUDIT_ID, GENERATION);

        verify(audit, never()).complete(anyInt(), any());
        verify(notificationService, never()).notifyAuditComplete(audit);
    }

    @Test
    void marksAuditAsFailed() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));

        auditProgressService.markFailed(AUDIT_ID, GENERATION);

        verify(audit).markFailed();
    }

    @Test
    void doesNotMarkFailedWhenAlreadyCancelled() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.isCancelled()).willReturn(true);

        auditProgressService.markFailed(AUDIT_ID, GENERATION);

        verify(audit, never()).markFailed();
    }

    @Test
    void doesNotMarkFailedWhenGenerationIsStale() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.getGeneration()).willReturn(1);

        auditProgressService.markFailed(AUDIT_ID, GENERATION);

        verify(audit, never()).markFailed();
    }

    @Test
    void markFailedWithoutGenerationIgnoresGenerationButRespectsCancelled() {
        // 자율점검 법령 매핑 실패·서버 재시작 복구처럼 특정 실행 세대를 모르는 호출부용
        // 오버로드. 세대는 안 보지만 취소된 감사는 여전히 건드리지 않는다.
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.isCancelled()).willReturn(true);

        auditProgressService.markFailed(AUDIT_ID);

        verify(audit, never()).markFailed();
    }

    @Test
    void isCancelledReflectsAuditState() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.isCancelled()).willReturn(true);

        assertThat(auditProgressService.isCancelled(AUDIT_ID, GENERATION)).isTrue();
    }

    @Test
    void isCancelledReturnsTrueWhenGenerationIsStale() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.getGeneration()).willReturn(1);

        assertThat(auditProgressService.isCancelled(AUDIT_ID, GENERATION)).isTrue();
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdForUpdate(AUDIT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                auditProgressService.markInProgress(AUDIT_ID, GENERATION)
        ).isInstanceOf(AuditNotFoundException.class);
    }
}
