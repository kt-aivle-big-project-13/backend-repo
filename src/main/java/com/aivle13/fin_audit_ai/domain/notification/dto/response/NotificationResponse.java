package com.aivle13.fin_audit_ai.domain.notification.dto.response;

import com.aivle13.fin_audit_ai.domain.notification.entity.NotificationEntity;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotifType type,
        String title,
        String message,
        boolean isRead,
        LocalDateTime sentAt,
        Long auditId
) {
    public static NotificationResponse from(NotificationEntity notification) {
        String title = switch (notification.getNotifType()) {
            case LAW_REVISION -> "법령 개정 안내";
            case REAUDIT_RECOMMEND -> "재감사 권고";
            case AUDIT_COMPLETE -> "감사 완료";
        };

        String message = switch (notification.getNotifType()) {
            case LAW_REVISION ->
                    notification.getRevision().getTitle() + " 관련 법령·고시가 개정되었습니다.";
            case REAUDIT_RECOMMEND ->
                    "'" + notification.getAudit().getAuditName() + "' 모델은 감사 결과 재감사가 필요합니다.";
            case AUDIT_COMPLETE ->
                    "'" + notification.getAudit().getAuditName() + "' 감사가 완료되었습니다.";
        };

        Long auditId = notification.getAudit() != null ? notification.getAudit().getId() : null;

        return new NotificationResponse(
                notification.getId(),
                notification.getNotifType(),
                title,
                message,
                notification.isRead(),
                notification.getSentAt(),
                auditId
        );
    }
}