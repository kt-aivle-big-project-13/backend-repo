package com.aivle13.fin_audit_ai.domain.user.dto.response.password;

public record PasswordResetResponse(
        String message
) {

    public static PasswordResetResponse success() {
        return new PasswordResetResponse(
                "비밀번호가 변경되었습니다."
        );
    }
}