package com.aivle13.fin_audit_ai.domain.user.dto.request.password;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(

        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인값을 입력해주세요.")
        String newPasswordConfirm

) {
}