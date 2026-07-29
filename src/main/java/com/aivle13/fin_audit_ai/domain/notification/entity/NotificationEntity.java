package com.aivle13.fin_audit_ai.domain.notification.entity;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.law.entity.LawRevisionEntity;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifChannel;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifStatus;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 법령 개정 또는 재감사 권고를 사용자에게 전달한 알림 발송 기록.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications",
        check = @CheckConstraint(
                name = "ck_notifications_type_reference",
                constraint = "(notif_type = 'LAW_REVISION' AND revision_id IS NOT NULL AND audit_id IS NULL) "
                        + "OR (notif_type = 'REAUDIT_RECOMMEND' AND audit_id IS NOT NULL AND revision_id IS NULL) "
                        + "OR (notif_type = 'AUDIT_COMPLETE' AND audit_id IS NOT NULL AND revision_id IS NULL)"))
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    // 재감사 권고 알림 시 NULL 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_id")
    private LawRevisionEntity revision;

    // 법령 개정 알림 시 NULL 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @Enumerated(EnumType.STRING)
    @Column(name = "notif_type", nullable = false, length = 20)
    private NotifType notifType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotifChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotifStatus status;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    // 알림 벨(피드)에서의 읽음 여부. channel/status는 발송 기록이고, 이건 별개로 사용자가
    // 인앱에서 확인했는지를 나타낸다.
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    public static NotificationEntity ofLawRevision(UserEntity user, LawRevisionEntity revision,
                                                   NotifChannel channel, NotifStatus status, LocalDateTime sentAt) {
        Objects.requireNonNull(revision, "revision must not be null for LAW_REVISION notification");
        NotificationEntity notification = new NotificationEntity();
        notification.user = user;
        notification.revision = revision;
        notification.notifType = NotifType.LAW_REVISION;
        notification.channel = channel;
        notification.status = status;
        notification.sentAt = sentAt;
        return notification;
    }

    public static NotificationEntity ofReauditRecommend(UserEntity user, AuditEntity audit,
                                                        NotifChannel channel, NotifStatus status, LocalDateTime sentAt) {
        Objects.requireNonNull(audit, "audit must not be null for REAUDIT_RECOMMEND notification");
        NotificationEntity notification = new NotificationEntity();
        notification.user = user;
        notification.audit = audit;
        notification.notifType = NotifType.REAUDIT_RECOMMEND;
        notification.channel = channel;
        notification.status = status;
        notification.sentAt = sentAt;
        return notification;
    }

    public static NotificationEntity ofAuditComplete(UserEntity user, AuditEntity audit,
                                                     NotifChannel channel, NotifStatus status, LocalDateTime sentAt) {
        Objects.requireNonNull(audit, "audit must not be null for AUDIT_COMPLETE notification");
        NotificationEntity notification = new NotificationEntity();
        notification.user = user;
        notification.audit = audit;
        notification.notifType = NotifType.AUDIT_COMPLETE;
        notification.channel = channel;
        notification.status = status;
        notification.sentAt = sentAt;
        return notification;
    }

    public void markRead() {
        this.isRead = true;
    }
}