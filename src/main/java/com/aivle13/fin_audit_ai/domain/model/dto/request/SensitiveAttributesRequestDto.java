package com.aivle13.fin_audit_ai.domain.model.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SensitiveAttributesRequestDto(
        @NotEmpty List<String> sensitiveAttributes
) {
}
