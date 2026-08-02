package com.aivle13.fin_audit_ai.domain.objection.dto.request;

import org.springframework.web.multipart.MultipartFile;

public record ObjectionImportRequest(
        MultipartFile file
) {
}