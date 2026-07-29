package com.aivle13.fin_audit_ai.domain.audit.event;

import com.aivle13.fin_audit_ai.domain.audit.service.core.AuditProgressService;
import com.aivle13.fin_audit_ai.domain.law.service.AuditLawMappingService;
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
public class SelfCheckLawMappingEventListener {

    private final AuditLawMappingService auditLawMappingService;
    private final AuditProgressService auditProgressService;

    @Async("auditTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(SelfCheckAnswersSubmittedEvent event) {
        try {
            auditLawMappingService.mapFromSelfCheckAnswers(event.auditId());

            log.info(
                    "자율점검 기반 법령 매핑 완료: auditId={}",
                    event.auditId()
            );
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
