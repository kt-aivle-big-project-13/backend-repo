package com.aivle13.fin_audit_ai.domain.user.entity;

import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String institution;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "law_email_enabled", nullable = false,
            columnDefinition = "boolean not null default true")
    private boolean lawEmailEnabled = true;

    @Column(name = "reaudit_alert_enabled", nullable = false)
    private boolean reauditAlertEnabled = true;

    @Column(name = "audit_complete_alert_enabled", nullable = false,
            columnDefinition = "boolean not null default true")
    private boolean auditCompleteAlertEnabled = true;

    @Column(name = "audit_fail_alert_enabled", nullable = false,
            columnDefinition = "boolean not null default true")
    private boolean auditFailAlertEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 소프트 삭제. 탈퇴해도 감사(Audit) 기록의 소유자 연결은 그대로 보존해야 해서
    // row는 남기고 비활성화만 한다.
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static UserEntity create(
            String name,
            String institution,
            String email,
            String passwordHash,
            UserRole role
    ) {
        UserEntity user = new UserEntity();
        user.name = name;
        user.institution = institution;
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = role;
        user.lawEmailEnabled = true;
        user.reauditAlertEnabled = true;
        user.auditCompleteAlertEnabled = true;
        user.auditFailAlertEnabled = true;

        return user;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void updateNotificationPreferences(
            boolean lawEmailEnabled,
            boolean reauditAlertEnabled,
            boolean auditCompleteAlertEnabled,
            boolean auditFailAlertEnabled
    ) {
        this.lawEmailEnabled = lawEmailEnabled;
        this.reauditAlertEnabled = reauditAlertEnabled;
        this.auditCompleteAlertEnabled = auditCompleteAlertEnabled;
        this.auditFailAlertEnabled = auditFailAlertEnabled;
    }

    // 회원 탈퇴(소프트 삭제). 이메일은 unique 제약이 걸려 있어 그대로 두면 같은
    // 이메일로 재가입이 막히므로, 로그인에는 못 쓰지만 유일성은 보장되는 값으로
    // 익명화한다. 이름·기관 등 다른 정보는 감사 이력 추적을 위해 그대로 둔다.
    public void withdraw() {
        this.isActive = false;
        this.deletedAt = LocalDateTime.now();
        this.email = "withdrawn_" + this.id + "@withdrawn.local";
    }
}