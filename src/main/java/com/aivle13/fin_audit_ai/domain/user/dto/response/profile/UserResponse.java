package com.aivle13.fin_audit_ai.domain.user.dto.response.profile;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String email,
        String name,
        String password,
        UserRole role,
        String institution,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        boolean lawEmailEnabled,
        boolean reauditAlertEnabled,
        boolean auditCompleteAlertEnabled
) {
    @SuppressWarnings("java:S2068")     // 실제 비밀번호가 아닌 마스킹 표시용 상수
    private static final String MASKED_PASSWORD = "********";

    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                MASKED_PASSWORD,
                user.getRole(),
                user.getInstitution(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.isLawEmailEnabled(),
                user.isReauditAlertEnabled(),
                user.isAuditCompleteAlertEnabled()
        );
    }
}