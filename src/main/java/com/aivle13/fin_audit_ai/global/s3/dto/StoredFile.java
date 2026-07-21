package com.aivle13.fin_audit_ai.global.s3.dto;

public record StoredFile(
        String s3Key,
        String originalName,
        String contentType,
        long size
) {
}
