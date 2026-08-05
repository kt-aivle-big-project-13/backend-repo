package com.aivle13.fin_audit_ai.domain.audit.dto.request.selfcheck;

import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckAnswerValue;
import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckItemCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// 21문항 중 응답한 것만 보내면 된다(미응답 문항은 아예 안 보냄) — 부분 제출을 허용한다.
public record SelfCheckAnswerSaveRequest(
        @NotEmpty
        @Valid
        List<Item> answers
) {
    public record Item(
            @NotNull SelfCheckItemCode itemCode,
            @NotNull SelfCheckAnswerValue answer
    ) {
    }
}
