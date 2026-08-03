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
        AuditStartedEvent event = new AuditStartedEvent(AUDIT_ID);

        listener.handle(event);

        InOrder inOrder = inOrder(
                auditProgressService,
                shapAnalysisService,
                fairnessAnalysisService
        );

        inOrder.verify(auditProgressService)
                .markInProgress(AUDIT_ID);
        inOrder.verify(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);
        inOrder.verify(auditProgressService)
                .markShapCompleted(AUDIT_ID);
        inOrder.verify(fairnessAnalysisService)
                .analyzeAndSave(AUDIT_ID);
        inOrder.verify(auditProgressService)
                .markFairnessCompleted(AUDIT_ID);

        verify(auditProgressService, never())
                .markFailed(AUDIT_ID);
    }

    @Test
    void skipsShapAnalysisWhenAlreadyCancelledBeforeStarting() {
        given(auditProgressService.isCancelled(AUDIT_ID))
                .willReturn(true);

        listener.handle(new AuditStartedEvent(AUDIT_ID));

        verify(auditProgressService)
                .markInProgress(AUDIT_ID);
        verifyNoInteractions(shapAnalysisService);
        verifyNoInteractions(fairnessAnalysisService);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID);
    }

    @Test
    void skipsFairnessAnalysisWhenCancelledDuringShapAnalysis() {
        given(auditProgressService.isCancelled(AUDIT_ID))
                .willReturn(false, true);

        listener.handle(new AuditStartedEvent(AUDIT_ID));

        verify(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);
        verify(auditProgressService)
                .markShapCompleted(AUDIT_ID);
        verifyNoInteractions(fairnessAnalysisService);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID);
    }

    @Test
    void marksAuditAsFailedWhenShapAiServerReturnsErrorAndDoesNotRunFairness() {
        willThrow(new AiServerErrorException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);

        listener.handle(new AuditStartedEvent(AUDIT_ID));

        verify(auditProgressService)
                .markInProgress(AUDIT_ID);
        verify(auditProgressService)
                .markFailed(AUDIT_ID);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID);
        verifyNoInteractions(fairnessAnalysisService);
    }

    @Test
    void marksAuditAsFailedWhenShapAiServerTimesOutAndDoesNotRunFairness() {
        willThrow(new AiServerTimeoutException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);

        listener.handle(new AuditStartedEvent(AUDIT_ID));

        verify(auditProgressService)
                .markInProgress(AUDIT_ID);
        verify(auditProgressService)
                .markFailed(AUDIT_ID);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID);
        verifyNoInteractions(fairnessAnalysisService);
    }

    @Test
    void marksAuditAsFailedWhenUnexpectedErrorOccursDuringShap() {
        willThrow(new IllegalStateException("unexpected error"))
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);

        listener.handle(new AuditStartedEvent(AUDIT_ID));

        verify(auditProgressService)
                .markFailed(AUDIT_ID);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID);
        verifyNoInteractions(fairnessAnalysisService);
    }

    @Test
    void marksAuditAsFailedWhenFairnessAnalysisFails() {
        willThrow(new AiServerErrorException())
                .given(fairnessAnalysisService)
                .analyzeAndSave(AUDIT_ID);

        listener.handle(new AuditStartedEvent(AUDIT_ID));

        verify(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);
        verify(auditProgressService)
                .markShapCompleted(AUDIT_ID);
        verify(auditProgressService)
                .markFailed(AUDIT_ID);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID);
    }

    @Test
    void doesNotPropagateExceptionWhenFailedStatusUpdateAlsoFails() {
        willThrow(new AiServerErrorException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);

        willThrow(new IllegalStateException("status update failed"))
                .given(auditProgressService)
                .markFailed(AUDIT_ID);

        assertThatCode(() ->
                listener.handle(new AuditStartedEvent(AUDIT_ID))
        ).doesNotThrowAnyException();

        verify(auditProgressService, times(3))
                .markFailed(AUDIT_ID);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID);
    }

    @Test
    void marksAuditAsFailedWithoutCallingAiServerWhenAiIsDisabled() {
        given(aiServerProperties.enabled())
                .willReturn(false);

        listener.handle(new AuditStartedEvent(AUDIT_ID));

        verify(auditProgressService)
                .markFailed(AUDIT_ID);
        verify(auditProgressService, never())
                .markInProgress(AUDIT_ID);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID);
        verifyNoInteractions(shapAnalysisService);
        verifyNoInteractions(fairnessAnalysisService);
    }

    @Test
    void retriesFailedStatusUpdateAndStopsWhenRetrySucceeds() {
        willThrow(new AiServerErrorException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);

        willThrow(new IllegalStateException("temporary failure"))
                .willDoNothing()
                .given(auditProgressService)
                .markFailed(AUDIT_ID);

        listener.handle(new AuditStartedEvent(AUDIT_ID));

        verify(auditProgressService, times(2))
                .markFailed(AUDIT_ID);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID);
    }
}
