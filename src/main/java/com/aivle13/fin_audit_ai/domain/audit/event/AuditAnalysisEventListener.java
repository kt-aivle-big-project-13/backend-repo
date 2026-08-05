package com.aivle13.fin_audit_ai.domain.audit.event;

import com.aivle13.fin_audit_ai.domain.audit.service.core.AuditProgressService;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPreGenerationService;
import com.aivle13.fin_audit_ai.domain.audit.service.fairness.FairnessAnalysisService;
import com.aivle13.fin_audit_ai.domain.audit.service.explainability.ShapAnalysisService;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.ai.config.AiServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class AuditAnalysisEventListener {

    private static final int FAILED_STATUS_MAX_ATTEMPTS = 3;

    // 병렬로 도는 분석(SHAP, Fairlearn) 개수. 완료 콜백이 몇 번째로 불렸는지와 비교해
    // "둘 다 끝났는지"를 판단하는 데 쓴다.
    private static final int ANALYSIS_COUNT = 2;

    private final ShapAnalysisService shapAnalysisService;
    private final FairnessAnalysisService fairnessAnalysisService;
    private final AuditProgressService auditProgressService;
    private final AiServerProperties aiServerProperties;
    private final ReportPreGenerationService reportPreGenerationService;
    private final Executor auditAnalysisExecutor;

    // @RequiredArgsConstructor 대신 직접 쓴다 — auditAnalysisExecutor는 같은 타입(Executor)의
    // 빈이 여럿(auditTaskExecutor, reportTaskExecutor)이라 @Qualifier로 어떤 풀을 주입받을지
    // 명시해야 하는데, Lombok이 생성하는 생성자로는 필드의 @Qualifier가 그대로 옮겨지지 않는다.
    public AuditAnalysisEventListener(
            ShapAnalysisService shapAnalysisService,
            FairnessAnalysisService fairnessAnalysisService,
            AuditProgressService auditProgressService,
            AiServerProperties aiServerProperties,
            ReportPreGenerationService reportPreGenerationService,
            @Qualifier("auditAnalysisExecutor") Executor auditAnalysisExecutor
    ) {
        this.shapAnalysisService = shapAnalysisService;
        this.fairnessAnalysisService = fairnessAnalysisService;
        this.auditProgressService = auditProgressService;
        this.aiServerProperties = aiServerProperties;
        this.reportPreGenerationService = reportPreGenerationService;
        this.auditAnalysisExecutor = auditAnalysisExecutor;
    }

    @Async("auditTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(AuditStartedEvent event) {
        Long auditId = event.auditId();
        int generation = event.generation();

        if (!aiServerProperties.enabled()) {
            log.warn(
                    "AI 서버 비활성화로 감사 분석을 실행할 수 없습니다: auditId={}",
                    auditId
            );

            markFailedSafely(auditId, generation);
            return;
        }

        try {
            auditProgressService.markInProgress(auditId, generation);

            if (auditProgressService.isCancelled(auditId, generation)) {
                return;
            }

            long parallelStart = System.currentTimeMillis();
            runAnalysesInParallel(auditId, generation);

            log.info(
                    "SHAP·Fairlearn 병렬 분석 및 결과 저장 완료: auditId={}, totalElapsedMs={}",
                    auditId,
                    System.currentTimeMillis() - parallelStart
            );

            // 결과 화면에서 다운로드를 누른 뒤에 만들면 사용자가 생성 시간을 그대로 기다린다.
            // 재료가 갖춰진 지금 미리 만들어 둔다. 실패해도 감사는 성공으로 남는다.
            reportPreGenerationService.preGenerateAfterAnalysis(auditId);
        } catch (BusinessException exception) {
            markFailedSafely(auditId, generation);

            log.error(
                    "감사 분석 실패: auditId={}, errorCode={}",
                    auditId,
                    exception.getErrorCode().getCode(),
                    exception
            );
        } catch (RuntimeException exception) {
            markFailedSafely(auditId, generation);

            log.error(
                    "예상하지 못한 감사 분석 오류: auditId={}",
                    auditId,
                    exception
            );
        }
    }

    /**
     * SHAP과 Fairlearn은 같은 모델·데이터셋을 각자 독립적으로 AI 서버에 요청할 뿐 서로의
     * 결과에 의존하지 않는다. 순차 실행 대신 동시에 실행해 전체 대기 시간을 "더 오래 걸리는
     * 쪽 하나"만큼으로 줄인다.
     *
     * <p>어느 쪽이 먼저 끝나는지는 그때그때 다르므로, "완료된 개수"를 기준으로 기존
     * {@code markShapCompleted}(1번째 완료 → 3단계 이동) / {@code markFairnessCompleted}
     * (2번째 완료 → 4단계 이동 + 판정)를 그대로 재사용한다. 메서드 이름과 실제로 먼저 끝난
     * 분석 종류가 다를 수 있지만, 두 메서드는 진행 단계를 한 칸씩 전진시키는 역할만 하고
     * 최종 판정({@code markFairnessCompleted} 내부)은 그 시점에 이미 두 분석 결과가 모두
     * 저장된 뒤이므로 안전하다.
     *
     * <p>취소 판단은 이 메서드 진입 전(handle 상단)에 한 번만 확인한다. 두 분석을 동시에
     * 제출한 뒤에는 이미 AI 서버로 요청이 나가 있으므로, 기존처럼 "SHAP 실패 시 Fairlearn
     * 자체를 부르지 않는" 조기 중단은 더 이상 불가능하다 — 병렬화로 얻는 시간 단축의
     * 트레이드오프로 감수한다(실패하면 어차피 감사는 FAILED로 표시된다).
     */
    private void runAnalysesInParallel(Long auditId, int generation) {
        AtomicInteger completedCount = new AtomicInteger(0);

        CompletableFuture<Void> shapFuture = CompletableFuture
                .runAsync(
                        () -> timed(
                                "SHAP",
                                auditId,
                                () -> shapAnalysisService.analyzeAndSave(auditId, generation)
                        ),
                        auditAnalysisExecutor
                )
                .thenRun(() -> onAnalysisCompleted(auditId, generation, completedCount));

        CompletableFuture<Void> fairnessFuture = CompletableFuture
                .runAsync(
                        () -> timed(
                                "Fairlearn",
                                auditId,
                                () -> fairnessAnalysisService.analyzeAndSave(auditId, generation)
                        ),
                        auditAnalysisExecutor
                )
                .thenRun(() -> onAnalysisCompleted(auditId, generation, completedCount));

        try {
            CompletableFuture.allOf(shapFuture, fairnessFuture).join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    // 병렬 실행이 실제로 겹쳐 도는지(동시 시작), 각자 얼마나 걸리는지를 로그만으로 눈으로
    // 확인할 수 있도록 시작/종료 시각과 소요시간을 남긴다. 스레드 이름(콘솔 로그의
    // [audit-analysis-N])까지 함께 보면 두 분석이 서로 다른 스레드에서 겹쳐 실행되는지도
    // 바로 확인된다.
    private void timed(String label, Long auditId, Runnable task) {
        long start = System.currentTimeMillis();
        log.info("{} 분석 시작: auditId={}", label, auditId);
        try {
            task.run();
        } finally {
            log.info(
                    "{} 분석 종료: auditId={}, elapsedMs={}",
                    label,
                    auditId,
                    System.currentTimeMillis() - start
            );
        }
    }

    // AtomicInteger는 카운터 값의 순서(1 다음에 2)만 보장할 뿐, 그 뒤에 이어지는
    // markShapCompleted()/markFairnessCompleted() 호출까지 순서대로 실행되게 하진
    // 않는다 — 서로 다른 스레드에서 각각 호출되므로, count=2 쪽이 count=1 쪽보다
    // 먼저 끝나버리면 이미 완료(4단계+최종 판정)된 감사의 currentStep이 3으로
    // 되돌아가 버릴 수 있다. AI 서버 호출은 이미 끝난 뒤라 이 메서드는 가벼운 DB
    // 전이 두 건뿐이므로, completedCount를 락으로 삼아 순서만 강제한다 — 병렬로
    // 돌린 AI 호출 자체는 전혀 건드리지 않는다. completedCount는 감사 1건 실행마다
    // 새로 만드는 로컬 인스턴스라, 동시에 다른 감사가 분석 중이어도 서로 락을
    // 공유하지 않는다.
    private void onAnalysisCompleted(Long auditId, int generation, AtomicInteger completedCount) {
        synchronized (completedCount) {
            if (completedCount.incrementAndGet() < ANALYSIS_COUNT) {
                auditProgressService.markShapCompleted(auditId, generation);
            } else {
                auditProgressService.markFairnessCompleted(auditId, generation);
            }
        }
    }

    private void markFailedSafely(Long auditId, int generation) {
        RuntimeException lastException = null;

        for (int attempt = 1;
             attempt <= FAILED_STATUS_MAX_ATTEMPTS;
             attempt++) {

            try {
                auditProgressService.markFailed(auditId, generation);

                if (attempt > 1) {
                    log.info(
                            "감사 실패 상태 저장 재시도 성공: auditId={}, attempt={}",
                            auditId,
                            attempt
                    );
                }

                return;
            } catch (RuntimeException statusUpdateException) {
                lastException = statusUpdateException;

                log.warn(
                        "감사 실패 상태 저장 실패: auditId={}, attempt={}/{}",
                        auditId,
                        attempt,
                        FAILED_STATUS_MAX_ATTEMPTS,
                        statusUpdateException
                );
            }
        }

        log.error(
                "감사 실패 상태 저장 최종 실패: auditId={}, attempts={}",
                auditId,
                FAILED_STATUS_MAX_ATTEMPTS,
                lastException
        );
    }
}