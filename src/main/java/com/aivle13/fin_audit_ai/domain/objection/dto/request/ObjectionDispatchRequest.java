package com.aivle13.fin_audit_ai.domain.objection.dto.request;

import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ObjectionDispatchRequest(
        @NotNull(message = "처리 결과를 선택해주세요.")
        ObjectionDecision decision,

        @NotBlank(message = "고객 안내문 제목을 입력해주세요.")
        String letterTitle,

        @NotBlank(message = "고객 안내문 내용을 입력해주세요.")
        String letterBody,

        @NotBlank(message = "수신 이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String recipientEmail
) {
}