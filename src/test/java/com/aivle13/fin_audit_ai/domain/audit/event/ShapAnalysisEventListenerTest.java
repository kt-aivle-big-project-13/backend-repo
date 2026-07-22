package com.aivle13.fin_audit_ai.domain.audit.event;

import com.aivle13.fin_audit_ai.domain.audit.service.AuditProgressService;
import com.aivle13.fin_audit_ai.domain.audit.service.ShapAnalysisService;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ShapAnalysisEventListenerTest {

    private static final Long AUDIT_ID = 1L;

    @Mock
    private ShapAnalysisService shapAnalysisService;

    @Mock
    private AuditProgressService auditProgressService;

    @Mock
    private AiServerProperties aiServerProperties;

    @BeforeEach
    void setUp() {
        given(aiServerProperties.enabled())
                .willReturn(true);
    }

    @InjectMocks
    private ShapAnalysisEventListener listener;

    @Test
    void processesShapAnalysisAndMovesToNextStep() {
        AuditStartedEvent event = new AuditStartedEvent(AUDIT_ID);

        listener.handle(event);

        InOrder inOrder = inOrder(
                auditProgressService,
                shapAnalysisService
        );

        inOrder.verify(auditProgressService)
                .markInProgress(AUDIT_ID);
        inOrder.verify(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);
        inOrder.verify(auditProgressService)
                .markShapCompleted(AUDIT_ID);

        verify(auditProgressService, never())
                .markFailed(AUDIT_ID);
    }

    @Test
    void marksAuditAsFailedWhenAiServerReturnsError() {
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
    }

    @Test
    void marksAuditAsFailedWhenAiServerTimesOut() {
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
    }

    @Test
    void marksAuditAsFailedWhenUnexpectedErrorOccurs() {
        willThrow(new IllegalStateException("unexpected error"))
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID);

        listener.handle(new AuditStartedEvent(AUDIT_ID));

        verify(auditProgressService)
                .markFailed(AUDIT_ID);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID);
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