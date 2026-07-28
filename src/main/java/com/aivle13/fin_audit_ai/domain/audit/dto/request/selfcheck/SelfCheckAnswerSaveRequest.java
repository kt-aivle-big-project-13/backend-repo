package com.aivle13.fin_audit_ai.domain.audit.dto.request.selfcheck;

import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SelfCheckAnswerSaveRequest(
        @NotEmpty
        @Valid
        List<Item> answers
) {
    public record Item(
            @NotNull SelfCheckItemCode itemCode,
            @NotNull Boolean answer
    ) {
    }
}
