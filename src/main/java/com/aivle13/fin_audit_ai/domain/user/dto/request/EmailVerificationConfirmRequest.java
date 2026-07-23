package com.aivle13.fin_audit_ai.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerificationConfirmRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(
            max = 100,
            message = "이메일은 100자 이하로 입력해주세요."
    )
    private String email;

    @NotBlank(message = "인증번호는 필수입니다.")
    @Pattern(
            regexp = "^\\d{6}$",
            message = "인증번호는 6자리 숫자여야 합니다."
    )
    private String code;
}