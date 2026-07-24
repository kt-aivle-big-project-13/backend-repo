package com.aivle13.fin_audit_ai.domain.user.dto.response.email;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerificationConfirmResponse {

    private String message;

    public static EmailVerificationConfirmResponse success() {
        return new EmailVerificationConfirmResponse(
                "인증이 완료되었습니다."
        );
    }
}