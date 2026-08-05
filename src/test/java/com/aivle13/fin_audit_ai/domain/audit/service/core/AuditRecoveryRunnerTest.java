package com.aivle13.fin_audit_ai.domain.audit.service.core;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditRecoveryRunnerTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private AuditProgressService auditProgressService;

    @InjectMocks
    private AuditRecoveryRunner auditRecoveryRunner;

    @Test
    void marksOrphanedInProgressAuditsAsFailed() {
        AuditEntity first = mock(AuditEntity.class);
        AuditEntity second = mock(AuditEntity.class);
        given(first.getId()).willReturn(1L);
        given(second.getId()).willReturn(2L);
        given(auditRepository.findAllByStatus(AuditStatus.IN_PROGRESS))
                .willReturn(List.of(first, second));

        auditRecoveryRunner.run(new DefaultApplicationArguments());

        verify(auditProgressService).markFailed(1L);
        verify(auditProgressService).markFailed(2L);
    }

    @Test
    void doesNothingWhenNoOrphanedAuditsExist() {
        given(auditRepository.findAllByStatus(AuditStatus.IN_PROGRESS))
                .willReturn(List.of());

        auditRecoveryRunner.run(new DefaultApplicationArguments());

        verify(auditProgressService, never()).markFailed(anyLong());
    }
}
