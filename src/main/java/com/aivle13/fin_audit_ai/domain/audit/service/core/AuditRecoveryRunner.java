package com.aivle13.fin_audit_ai.domain.audit.service.core;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 감사 분석 도중 서버가 재시작되면 비동기 작업이 통째로 사라져 감사가 IN_PROGRESS
 * 상태로 영구히 남는다. 그대로 두면 프론트가 무한 폴링에 빠지므로, 기동 시점에
 * 고아 상태인 감사를 찾아 FAILED로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRecoveryRunner implements ApplicationRunner {

    private final AuditRepository auditRepository;
    private final AuditProgressService auditProgressService;

    @Override
    public void run(ApplicationArguments args) {
        List<AuditEntity> orphanedAudits =
                auditRepository.findAllByStatus(AuditStatus.IN_PROGRESS);

        if (orphanedAudits.isEmpty()) {
            return;
        }

        for (AuditEntity audit : orphanedAudits) {
            auditProgressService.markFailed(audit.getId());
        }

        log.warn(
                "서버 재시작으로 고아 상태(IN_PROGRESS)였던 감사 {}건을 FAILED로 전환했습니다: auditIds={}",
                orphanedAudits.size(),
                orphanedAudits.stream().map(AuditEntity::getId).toList()
        );
    }
}
