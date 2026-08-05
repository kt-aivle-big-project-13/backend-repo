package com.aivle13.fin_audit_ai.domain.audit.event;

import com.aivle13.fin_audit_ai.domain.audit.service.core.AuditProgressService;
import com.aivle13.fin_audit_ai.domain.law.service.mapping.AuditRegulationMappingService;
import com.aivle13.fin_audit_ai.domain.report.service.common.ReportPreGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 자율점검 응답 저장이 커밋된 뒤, 비동기로 관련 법령 조항 후보를 매핑한다.
 * 법령 매핑이 안 된 채로 보고서가 생성되면 준수 여부 판정이 통째로 빠진 보고서가
 * 조용히 만들어질 수 있으므로, 매핑 실패는 감사 자체의 실패(AuditStatus.FAILED)로 취급한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelfCheckRegulationMappingEventListener {

    private final AuditRegulationMappingService auditRegulationMappingService;
    private final AuditProgressService auditProgressService;
    private final ReportPreGenerationService reportPreGenerationService;

    @Async("auditTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(SelfCheckAnswersSubmittedEvent event) {
        try {
            auditRegulationMappingService.mapFromSelfCheckAnswers(event.auditId());

            log.info(
                    "자율점검 기반 법령 매핑 완료: auditId={}",
                    event.auditId()
            );

            // 규제준수 판정서와 개선 권고 가이드는 자율점검 응답과 법령 매핑이 있어야 만들 수
            // 있다. 재료가 갖춰진 지금 미리 만들어 두면 다운로드가 즉시 끝난다.
            reportPreGenerationService.preGenerateAfterSelfCheck(event.auditId());
        } catch (RuntimeException exception) {
            log.error(
                    "자율점검 기반 법령 매핑 실패, 감사를 실패 처리함: auditId={}",
                    event.auditId(),
                    exception
            );

            auditProgressService.markFailed(event.auditId());
        }
    }
}
