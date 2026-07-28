package com.aivle13.fin_audit_ai.global.s3.dto;

import java.io.InputStream;

public record DownloadedFile(
        InputStream content,
        String contentType,
        long contentLength
) {
}