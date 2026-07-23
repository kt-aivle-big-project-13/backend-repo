package com.aivle13.fin_audit_ai.domain.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(
                max = 50,
                message = "이름은 50자 이하로 입력해주세요."
        )
        String name,

        @NotBlank(message = "기업 이름은 필수입니다.")
        @Size(
                max = 100,
                message = "기업 이름은 100자 이하로 입력해주세요."
        )
        String institution,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(
                max = 100,
                message = "이메일은 100자 이하로 입력해주세요."
        )
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm,

        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
        boolean serviceTermsAgreed,

        @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
        boolean privacyTermsAgreed

) {
}