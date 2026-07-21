package com.aivle13.fin_audit_ai.domain.audit.dto.response;

import com.aivle13.fin_audit_ai.domain.audit.dto.UploadedFiles;

public record AuditUploadResponse(
        Long auditId,
        Long modelId,
        UploadedFiles uploadedFiles,
        String status
) { }
