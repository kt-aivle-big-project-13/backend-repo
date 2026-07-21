package com.aivle13.fin_audit_ai.domain.audit.dto;

public record AuditUploadResponseDto(
        Long auditId,
        Long modelId,
        UploadedFiles uploadedFiles,
        String status
) {
    public record UploadedFiles(boolean model, boolean auditDataset, boolean validationDataset) {}
}
