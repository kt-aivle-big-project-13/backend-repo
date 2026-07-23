package com.aivle13.fin_audit_ai.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupResponse {

    private String message;

    public static SignupResponse success() {
        return new SignupResponse("회원가입이 완료되었습니다.");
    }
}