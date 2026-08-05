package com.aivle13.fin_audit_ai.domain.audit.event;

import com.aivle13.fin_audit_ai.domain.audit.service.core.AuditProgressService;
import com.aivle13.fin_audit_ai.domain.law.service.mapping.AuditRegulationMappingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPreGenerationService;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SelfCheckRegulationMappingEventListenerTest {

    private static final Long AUDIT_ID = 21L;

    @Mock
    private AuditRegulationMappingService auditRegulationMappingService;

    @Mock
    private AuditProgressService auditProgressService;

    @Mock
    private ReportPreGenerationService reportPreGenerationService;

    @InjectMocks
    private SelfCheckRegulationMappingEventListener listener;

    @Test
    void mapsWhenEventReceived() {
        listener.handle(new SelfCheckAnswersSubmittedEvent(AUDIT_ID));

        verify(auditRegulationMappingService).mapFromSelfCheckAnswers(AUDIT_ID);
        verify(auditProgressService, never()).markFailed(AUDIT_ID);
    }

    @Test
    void swallowsExceptionSoItNeverPropagatesButMarksAuditFailed() {
        willThrow(new RuntimeException("AI 서버 오류"))
                .given(auditRegulationMappingService).mapFromSelfCheckAnswers(AUDIT_ID);

        assertThatCode(() -> listener.handle(new SelfCheckAnswersSubmittedEvent(AUDIT_ID)))
                .doesNotThrowAnyException();

        verify(auditProgressService).markFailed(AUDIT_ID);
    }
}
