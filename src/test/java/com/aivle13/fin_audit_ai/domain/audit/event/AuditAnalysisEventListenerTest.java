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
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPreGenerationService;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

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

    // 실제 병렬 실행 대신, 제출된 작업을 호출한 스레드에서 즉시 실행하는 동기 실행기로
    // 대체한다. SHAP·Fairlearn 이 진짜 동시에 도는지는 스레드풀 설정(AsyncConfig)의
    // 책임이고, 여기서는 "두 작업이 각자 결과를 어떻게 진행 상태에 반영하는지"라는
    // 오케스트레이션 로직만 결정적으로 검증한다. 코드 순서상 SHAP을 먼저 제출하므로,
    // 동기 실행기에서는 SHAP 쪽 완료 콜백이 항상 먼저 불린다.
    @Mock
    private Executor auditAnalysisExecutor;

    @BeforeEach
    void setUp() {
        given(aiServerProperties.enabled())
                .willReturn(true);

        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(auditAnalysisExecutor).execute(any());
    }

    @InjectMocks
    private AuditAnalysisEventListener listener;

    @Test
    void processesShapAndFairnessAnalysisInParallelAndCompletesInSubmissionOrder() {
        AuditStartedEvent event = new AuditStartedEvent(AUDIT_ID, GENERATION);

        listener.handle(event);

        verify(auditAnalysisExecutor, times(2)).execute(any());

        InOrder inOrder = inOrder(
                auditProgressService,
                shapAnalysisService,
                fairnessAnalysisService,
                reportPreGenerationService
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
        inOrder.verify(reportPreGenerationService)
                .preGenerateAfterAnalysis(AUDIT_ID);

        verify(auditProgressService, never())
                .markFailed(AUDIT_ID, GENERATION);
    }

    @Test
    void skipsBothAnalysesWhenAlreadyCancelledBeforeStarting() {
        given(auditProgressService.isCancelled(AUDIT_ID, GENERATION))
                .willReturn(true);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(auditProgressService)
                .markInProgress(AUDIT_ID, GENERATION);
        verifyNoInteractions(shapAnalysisService);
        verifyNoInteractions(fairnessAnalysisService);
        verifyNoInteractions(auditAnalysisExecutor);
        verify(auditProgressService, never())
                .markShapCompleted(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID, GENERATION);
    }

    // 병렬 실행에서는 두 분석이 이미 동시에 AI 서버로 나가 있으므로, 예전처럼 "SHAP 실패 시
    // Fairlearn 자체를 부르지 않는" 조기 중단은 더 이상 없다. Fairlearn은 그대로 끝까지
    // 실행되고(여기선 성공), 감사는 결국 FAILED로 표시된다.
    @Test
    void marksAuditAsFailedWhenShapAiServerReturnsErrorButFairnessStillRuns() {
        willThrow(new AiServerErrorException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(auditProgressService)
                .markInProgress(AUDIT_ID, GENERATION);
        verify(fairnessAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);
        verify(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID, GENERATION);
        verify(reportPreGenerationService, never())
                .preGenerateAfterAnalysis(AUDIT_ID);
    }

    @Test
    void marksAuditAsFailedWhenShapAiServerTimesOutButFairnessStillRuns() {
        willThrow(new AiServerTimeoutException())
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(fairnessAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);
        verify(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID, GENERATION);
    }

    @Test
    void marksAuditAsFailedWhenUnexpectedErrorOccursDuringShap() {
        willThrow(new IllegalStateException("unexpected error"))
                .given(shapAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);

        listener.handle(new AuditStartedEvent(AUDIT_ID, GENERATION));

        verify(fairnessAnalysisService)
                .analyzeAndSave(AUDIT_ID, GENERATION);
        verify(auditProgressService)
                .markFailed(AUDIT_ID, GENERATION);
        verify(auditProgressService, never())
                .markFairnessCompleted(AUDIT_ID, GENERATION);
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
        verifyNoInteractions(shapAnalysisService);
        verifyNoInteractions(fairnessAnalysisService);
        verifyNoInteractions(auditAnalysisExecutor);
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
    }
}