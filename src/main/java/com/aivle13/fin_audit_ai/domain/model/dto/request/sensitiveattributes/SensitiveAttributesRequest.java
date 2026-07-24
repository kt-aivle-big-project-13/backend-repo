package com.aivle13.fin_audit_ai.domain.model.dto.request.sensitiveattributes;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SensitiveAttributesRequest(
        @NotEmpty List<String> sensitiveAttributes
) {
}
