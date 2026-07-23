package com.aivle13.fin_audit_ai.domain.user.entity;

import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "law_sms_enabled", nullable = false)
    private boolean lawSmsEnabled = true;

    @Column(name = "reaudit_alert_enabled", nullable = false)
    private boolean reauditAlertEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

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
        user.lawSmsEnabled = true;
        user.reauditAlertEnabled = true;

        return user;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void updateNotificationPreferences(
            boolean lawSmsEnabled,
            boolean reauditAlertEnabled
    ) {
        this.lawSmsEnabled = lawSmsEnabled;
        this.reauditAlertEnabled = reauditAlertEnabled;
    }
}