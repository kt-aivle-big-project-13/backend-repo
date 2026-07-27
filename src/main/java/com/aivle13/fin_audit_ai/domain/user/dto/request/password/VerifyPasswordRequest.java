package com.aivle13.fin_audit_ai.domain.user.dto.request.password;

import jakarta.validation.constraints.NotBlank;

public record VerifyPasswordRequest(

        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword

) {
}