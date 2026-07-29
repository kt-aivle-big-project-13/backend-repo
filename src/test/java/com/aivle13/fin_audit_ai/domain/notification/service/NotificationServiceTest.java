package com.aivle13.fin_audit_ai.domain.notification.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.notification.dto.response.NotificationResponse;
import com.aivle13.fin_audit_ai.domain.notification.entity.NotificationEntity;
import com.aivle13.fin_audit_ai.domain.notification.repository.NotificationRepository;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifChannel;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifStatus;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.notification.NotificationNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 10L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuditEntity audit;

    @Mock
    private UserEntity user;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void 사용자의_알림_목록을_최신순으로_조회한다() {
        NotificationEntity notification = NotificationEntity.ofAuditComplete(
                user, audit, NotifChannel.IN_APP, NotifStatus.SENT, LocalDateTime.now());
        given(audit.getAuditName()).willReturn("신용평가 모델 A");
        given(notificationRepository.findByUser_IdOrderBySentAtDesc(USER_ID, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(notification)));

        PageResponse<NotificationResponse> result = notificationService.list(USER_ID, 1, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).type()).isEqualTo(NotifType.AUDIT_COMPLETE);
    }

    @Test
    void 안읽음_개수를_그대로_반환한다() {
        given(notificationRepository.countUnreadByUserId(USER_ID)).willReturn(3L);

        long result = notificationService.countUnread(USER_ID);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void 본인_알림을_단건_읽음_처리한다() {
        NotificationEntity notification = NotificationEntity.ofAuditComplete(
                user, audit, NotifChannel.IN_APP, NotifStatus.SENT, LocalDateTime.now());
        given(notificationRepository.findByIdAndUser_Id(NOTIFICATION_ID, USER_ID))
                .willReturn(Optional.of(notification));

        notificationService.markAsRead(USER_ID, NOTIFICATION_ID);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void 존재하지_않거나_본인_알림이_아니면_읽음_처리시_예외() {
        given(notificationRepository.findByIdAndUser_Id(NOTIFICATION_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, NOTIFICATION_ID))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void 전체_읽음_처리는_리포지토리에_위임한다() {
        notificationService.markAllAsRead(USER_ID);

        verify(notificationRepository).markAllAsReadByUserId(USER_ID);
    }

    @Test
    void 감사완료_알림설정이_켜져있으면_알림을_생성한다() {
        given(audit.getUser()).willReturn(user);
        given(user.isAuditCompleteAlertEnabled()).willReturn(true);

        notificationService.notifyAuditComplete(audit);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getNotifType()).isEqualTo(NotifType.AUDIT_COMPLETE);
        assertThat(captor.getValue().getChannel()).isEqualTo(NotifChannel.IN_APP);
    }

    @Test
    void 감사완료_알림설정이_꺼져있으면_알림을_생성하지_않는다() {
        given(audit.getUser()).willReturn(user);
        given(user.isAuditCompleteAlertEnabled()).willReturn(false);

        notificationService.notifyAuditComplete(audit);

        verify(notificationRepository, never()).save(any());
    }
}