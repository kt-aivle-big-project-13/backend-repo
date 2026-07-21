package com.aivle13.fin_audit_ai.domain.model.dto;

import java.util.List;

public record SensitiveAttributesResponseDto(
        Long modelId,
        List<String> sensitiveAttributes
) {
}
