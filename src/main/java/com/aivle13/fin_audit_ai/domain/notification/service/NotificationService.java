package com.aivle13.fin_audit_ai.domain.notification.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.notification.dto.response.NotificationResponse;
import com.aivle13.fin_audit_ai.domain.notification.entity.NotificationEntity;
import com.aivle13.fin_audit_ai.domain.notification.repository.NotificationRepository;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifChannel;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifStatus;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.notification.NotificationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 알림 벨 목록 (최신순 페이징)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);

        Page<NotificationEntity> result = notificationRepository.findByUser_IdOrderBySentAtDesc(userId, pageable);

        List<NotificationResponse> content = result.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        return PageResponse.of(result, content);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        NotificationEntity notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);

        notification.markRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    // 감사 완료 처리 시점(AuditProgressService)에서 호출되는 실제 알림 생성 지점.
    // 법령 개정/재감사 권고 알림은 이를 발생시키는 크롤러·배치가 아직 없어 생성 지점이 없다.
    @Transactional
    public void notifyAuditComplete(AuditEntity audit) {
        UserEntity user = audit.getUser();

        if (!user.isAuditCompleteAlertEnabled()) {
            return;
        }

        NotificationEntity notification = NotificationEntity.ofAuditComplete(
                user, audit, NotifChannel.IN_APP, NotifStatus.SENT, LocalDateTime.now());

        notificationRepository.save(notification);
    }
}