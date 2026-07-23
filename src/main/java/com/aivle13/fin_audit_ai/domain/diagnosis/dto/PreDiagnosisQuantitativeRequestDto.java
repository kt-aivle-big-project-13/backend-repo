package com.aivle13.fin_audit_ai.domain.diagnosis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PreDiagnosisQuantitativeRequestDto(
        @NotEmpty List<@Valid AnswerDto> answers
) {

    public record AnswerDto(
            @NotBlank String questionCode,
            boolean answer
    ) {
    }
}
