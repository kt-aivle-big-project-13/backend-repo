package com.aivle13.fin_audit_ai.domain.user.dto.response.password;

public record PasswordFindResponse(
        String message,
        long expiresIn
) {

    public static PasswordFindResponse success() {
        return new PasswordFindResponse(
                "비밀번호 재설정 링크가 이메일로 발송되었습니다.",
                1800L
        );
    }
}