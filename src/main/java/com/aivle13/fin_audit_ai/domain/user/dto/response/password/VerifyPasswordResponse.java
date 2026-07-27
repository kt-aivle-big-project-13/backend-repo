package com.aivle13.fin_audit_ai.domain.user.dto.response.password;

public record VerifyPasswordResponse(
        String message
) {

    public static VerifyPasswordResponse success() {
        return new VerifyPasswordResponse(
                "현재 비밀번호가 확인되었습니다."
        );
    }
}