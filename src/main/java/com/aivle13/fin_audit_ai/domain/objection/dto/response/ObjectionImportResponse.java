package com.aivle13.fin_audit_ai.domain.objection.dto.response;

import java.util.List;

public record ObjectionImportResponse(
        int importedCount,
        List<ObjectionSummaryResponse> objections
) {
}