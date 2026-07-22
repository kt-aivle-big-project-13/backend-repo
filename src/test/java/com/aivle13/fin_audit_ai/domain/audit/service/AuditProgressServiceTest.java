package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditProgressServiceTest {

    private static final Long AUDIT_ID = 1L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private AuditEntity audit;

    @InjectMocks
    private AuditProgressService auditProgressService;

    @Test
    void marksAuditAsInProgress() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        auditProgressService.markInProgress(AUDIT_ID);

        verify(audit).markInProgress();
    }

    @Test
    void movesAuditToFairnessStepWhenShapIsCompleted() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        auditProgressService.markShapCompleted(AUDIT_ID);

        verify(audit).moveToStep(3);
    }

    @Test
    void marksAuditAsFailed() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        auditProgressService.markFailed(AUDIT_ID);

        verify(audit).markFailed();
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                auditProgressService.markInProgress(AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);
    }
}