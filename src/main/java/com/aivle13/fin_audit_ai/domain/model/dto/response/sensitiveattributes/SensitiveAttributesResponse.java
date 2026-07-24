package com.aivle13.fin_audit_ai.domain.model.dto.response.sensitiveattributes;

import java.util.List;

public record SensitiveAttributesResponse(
        Long datasetId,
        List<String> sensitiveAttributes
) {
}
