package com.aivle13.fin_audit_ai.domain.auth.dto.response;

public record SignupResponse(
        String message
) {

    public static SignupResponse success() {
        return new SignupResponse(
                "회원가입이 완료되었습니다."
        );
    }
}