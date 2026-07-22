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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShapAnalysisEventListenerTest {

    private static final Long AUDIT_ID = 1L;

    @Mock
    private ShapAnalysisService shapAnalysisService;

    @Mock
    private AuditProgressService auditProgressService;

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

        verify(auditProgressService)
                .markFailed(AUDIT_ID);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID);
    }
}