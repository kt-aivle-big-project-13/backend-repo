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

/**
 * 법령 개정 또는 재감사 권고를 사용자에게 전달한 알림 발송 기록.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications")
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
}
