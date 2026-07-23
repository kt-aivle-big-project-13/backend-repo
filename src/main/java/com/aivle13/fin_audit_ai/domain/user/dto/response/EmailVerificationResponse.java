package com.aivle13.fin_audit_ai.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerificationResponse {

    private String message;
    private int expiresIn;

    public static EmailVerificationResponse success() {
        return new EmailVerificationResponse(
                "인증번호가 발송되었습니다.",
                300
        );
    }
}