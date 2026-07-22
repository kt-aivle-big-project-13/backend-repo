package com.aivle13.fin_audit_ai.domain.audit.event;

import com.aivle13.fin_audit_ai.domain.audit.service.AuditProgressService;
import com.aivle13.fin_audit_ai.domain.audit.service.ShapAnalysisService;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import com.aivle13.fin_audit_ai.global.ai.config.AiServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor

public class ShapAnalysisEventListener {

    private static final int FAILED_STATUS_MAX_ATTEMPTS = 3;

    private final ShapAnalysisService shapAnalysisService;
    private final AuditProgressService auditProgressService;
    private final AiServerProperties aiServerProperties;

    @Async("auditTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(AuditStartedEvent event) {
        Long auditId = event.auditId();

        if (!aiServerProperties.enabled()) {
            log.warn(
                    "AI 서버 비활성화로 SHAP 분석을 실행할 수 없습니다: auditId={}",
                    auditId
            );

            markFailedSafely(auditId);
            return;
        }

        try {
            auditProgressService.markInProgress(auditId);

            shapAnalysisService.analyzeAndSave(auditId);

            auditProgressService.markShapCompleted(auditId);

            log.info(
                    "SHAP 분석 및 결과 저장 완료: auditId={}",
                    auditId
            );
        } catch (BusinessException exception) {
            markFailedSafely(auditId);

            log.error(
                    "SHAP 분석 실패: auditId={}, errorCode={}",
                    auditId,
                    exception.getErrorCode().getCode(),
                    exception
            );
        } catch (RuntimeException exception) {
            markFailedSafely(auditId);

            log.error(
                    "예상하지 못한 SHAP 분석 오류: auditId={}",
                    auditId,
                    exception
            );
        }
    }

    private void markFailedSafely(Long auditId) {
        RuntimeException lastException = null;

        for (int attempt = 1;
             attempt <= FAILED_STATUS_MAX_ATTEMPTS;
             attempt++) {

            try {
                auditProgressService.markFailed(auditId);

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