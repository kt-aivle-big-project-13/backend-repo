package com.aivle13.fin_audit_ai.domain.model.dto.response;

import java.util.List;

public record SensitiveAttributesResponse(
        Long modelId,
        List<String> sensitiveAttributes
) {
}
