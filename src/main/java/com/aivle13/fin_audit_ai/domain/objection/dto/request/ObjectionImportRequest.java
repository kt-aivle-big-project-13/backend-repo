package com.aivle13.fin_audit_ai.domain.objection.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ObjectionImportRequest(
        @NotNull Long modelId,
        MultipartFile file
) {
}