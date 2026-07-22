package com.aivle13.fin_audit_ai.domain.audit.event;

import com.aivle13.fin_audit_ai.domain.audit.service.AuditProgressService;
import com.aivle13.fin_audit_ai.domain.audit.service.ShapAnalysisService;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ai-server",
        name = "enabled",
        havingValue = "true"
)
public class ShapAnalysisEventListener {

    private final ShapAnalysisService shapAnalysisService;
    private final AuditProgressService auditProgressService;

    @Async("auditTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(AuditStartedEvent event) {
        Long auditId = event.auditId();

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
        try {
            auditProgressService.markFailed(auditId);
        } catch (RuntimeException statusUpdateException) {
            log.error(
                    "감사 실패 상태 저장 실패: auditId={}",
                    auditId,
                    statusUpdateException
            );
        }
    }
}