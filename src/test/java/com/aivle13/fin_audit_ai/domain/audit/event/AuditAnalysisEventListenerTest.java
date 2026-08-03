package com.aivle13.fin_audit_ai.domain.audit.event;

import com.aivle13.fin_audit_ai.domain.audit.service.core.AuditProgressService;
import com.aivle13.fin_audit_ai.domain.audit.service.fairness.FairnessAnalysisService;
import com.aivle13.fin_audit_ai.domain.audit.service.explainability.ShapAnalysisService;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aivle13.fin_audit_ai.global.ai.config.AiServerProperties;
import org.junit.jupiter.api.BeforeEach;
import com.aivle13.fin_audit_ai.domain.report.service.ReportPreGenerationService;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AuditAnalysisEventListenerTest {

    private static final Long AUDIT_ID = 1L;
    private static final int GENERATION = 0;

    @Mock
    private ShapAnalysisService shapAnalysisService;

    @Mock
    private FairnessAnalysisService fairnessAnalysisService;

    @Mock
    private AuditProgressService auditProgressService;

    @Mock
    private AiServerProperties aiServerProperties;

    @Mock
    private ReportPreGenerationService reportPreGenerationService;

    @BeforeEach
    void setUp() {
        given(aiServerProperties.enabled())
                .willReturn(true);
    }

    @InjectMocks
    private AuditAnalysisEventListener listener;

    @Test
    void processesShapAndFairnessAnalysisInOrder() {
        AuditStartedEvent event = new AuditStartedEvent(AUDIT_ID, GENERATION);

        listener.handle(event);

        InOrder inOrder = inOrder(
                auditProgressService,
                shapAnalysisService,
                fairnessAnalysisService
        );

        inOrder.verify(auditProgressService)
                .markInProgress(AUDIT_ID, GENERATION);
        inOrder.verify(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);
        inOrder.verify(auditProgressService)
                .markShapCompleted(AUDIT_ID, GENERATION);
        inOrder.verify(fairnessAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);
        inOrder.verify(auditProgressService)
                .markFairnessCompleted(AUDIT_ID, GENERATION);

        verify(auditProgressService, never())
                .markFailed(AUDIT_ID, GENERATION);
    }

    @Test
    void skipsShapAnalysisWhenAlreadyCancelledBeforeStarting() {
        given(auditProgressService.isCancelled(AUDIT_ID, GENERATION))
                .willReturn(true);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(auditProgressService)
                .markInProgress(AUDIT_ID, GENERATION);
        verifyNoInteractions(shapAnalysisService);
        verifyNoInteractions(fairnessAnalysisService);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID, GENERATION);
    }

    @Test
    void skipsFairnessAnalysisWhenCancelledDuringShapAnalysis() {
        given(auditProgressService.isCancelled(AUDIT_ID, GENERATION))
                .willReturn(false, true);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);
        verify(auditProgressService)
                .markShapCompleted(AUDIT_ID, GENERATION);
        verifyNoInteractions(fairnessAnalysisService);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID, GENERATION);
    }

    @Test
    void marksAuditAsFailedWhenShapAiServerReturnsErrorAndDoesNotRunFairness() {
        willThrow(new AiServerErrorException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(auditProgressService)
                .markInProgress(AUDIT_ID, GENERATION);
        verify(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID, GENERATION);
        verifyNoInteractions(fairnessAnalysisService);
    }

    @Test
    void marksAuditAsFailedWhenShapAiServerTimesOutAndDoesNotRunFairness() {
        willThrow(new AiServerTimeoutException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(auditProgressService)
                .markInProgress(AUDIT_ID, GENERATION);
        verify(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID, GENERATION);
        verifyNoInteractions(fairnessAnalysisService);
    }

    @Test
    void marksAuditAsFailedWhenUnexpectedErrorOccursDuringShap() {
        willThrow(new IllegalStateException("unexpected error"))
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID, GENERATION);
        verifyNoInteractions(fairnessAnalysisService);
    }

    @Test
    void marksAuditAsFailedWhenFairnessAnalysisFails() {
        willThrow(new AiServerErrorException())
                .given(fairnessAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);
        verify(auditProgressService)
                .markShapCompleted(AUDIT_ID, GENERATION);
        verify(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID, GENERATION);
    }

    @Test
    void doesNotPropagateExceptionWhenFailedStatusUpdateAlsoFails() {
        willThrow(new AiServerErrorException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);

        willThrow(new IllegalStateException("status update failed"))
                .given(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);

        assertThatCode(() ->
                listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION))
        ).doesNotThrowAnyException();

        verify(auditProgressService, times(3))
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID, GENERATION);
    }

    @Test
    void marksAuditAsFailedWithoutCallingAiServerWhenAiIsDisabled() {
        given(aiServerProperties.enabled())
                .willReturn(false);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markInProgress(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID, GENERATION);
        verifyNoInteractions(shapAnalysisService);
        verifyNoInteractions(fairnessAnalysisService);
    }

    @Test
    void retriesFailedStatusUpdateAndStopsWhenRetrySucceeds() {
        willThrow(new AiServerErrorException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);

        willThrow(new IllegalStateException("temporary failure"))
                .willDoNothing()
                .given(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(auditProgressService, times(2))
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID, GENERATION);
    }
}
