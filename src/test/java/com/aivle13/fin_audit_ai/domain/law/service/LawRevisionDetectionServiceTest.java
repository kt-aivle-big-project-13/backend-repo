package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawRevisionEntity;
import com.aivle13.fin_audit_ai.domain.law.type.RevisionType;
import com.aivle13.fin_audit_ai.domain.notification.service.NotificationService;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.law.LawApiErrorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LawRevisionDetectionServiceTest {

    private static final String LAW_NAME = "AI 기본법";
    private static final String OFFICIAL_LAW_NAME = "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법";
    private static final String OFFICIAL_DECREE_NAME = "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법 시행령";

    @Mock
    private LawRevisionApplier lawRevisionApplier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LawRevisionDetectionService lawRevisionDetectionService;

    @Test
    void detectAndApplyContinuesWithOtherLawWhenOneLawApiCallFails() {
        given(lawRevisionApplier.applyForLaw(LAW_NAME, OFFICIAL_LAW_NAME)).willThrow(new LawApiErrorException());
        given(lawRevisionApplier.applyForLaw("AI 기본법 시행령", OFFICIAL_DECREE_NAME)).willReturn(List.of());

        List<LawRevisionEntity> revisions = lawRevisionDetectionService.detectAndApply();

        assertThat(revisions).isEmpty();
        verify(userRepository, never()).findByIsActiveTrue();
    }

    @Test
    void notifiesAllActiveUsersForEachDetectedRevision() {
        LawRevisionEntity revision = LawRevisionEntity.of(
                "law.go.kr", "AI 기본법 제31조 개정", RevisionType.AMENDMENT,
                LocalDate.of(2026, 7, 21), LocalDateTime.now()
        );

        given(lawRevisionApplier.applyForLaw(LAW_NAME, OFFICIAL_LAW_NAME)).willReturn(List.of(revision));
        given(lawRevisionApplier.applyForLaw("AI 기본법 시행령", OFFICIAL_DECREE_NAME)).willReturn(List.of());

        UserEntity activeUser1 = UserEntity.create("김철수", "핀테크뱅크", "a@test.com", "hash", UserRole.USER);
        UserEntity activeUser2 = UserEntity.create("이영희", "핀테크뱅크", "b@test.com", "hash", UserRole.USER);
        given(userRepository.findByIsActiveTrue()).willReturn(List.of(activeUser1, activeUser2));

        List<LawRevisionEntity> revisions = lawRevisionDetectionService.detectAndApply();

        assertThat(revisions).hasSize(1);
        verify(notificationService).notifyLawRevision(activeUser1, revision);
        verify(notificationService).notifyLawRevision(activeUser2, revision);
    }

    @Test
    void doesNotLookUpUsersWhenNoRevisionsDetected() {
        given(lawRevisionApplier.applyForLaw(LAW_NAME, OFFICIAL_LAW_NAME)).willReturn(List.of());
        given(lawRevisionApplier.applyForLaw("AI 기본법 시행령", OFFICIAL_DECREE_NAME)).willReturn(List.of());

        List<LawRevisionEntity> revisions = lawRevisionDetectionService.detectAndApply();

        assertThat(revisions).isEmpty();
        verify(userRepository, never()).findByIsActiveTrue();
        verify(notificationService, never()).notifyLawRevision(any(), any());
    }
}
