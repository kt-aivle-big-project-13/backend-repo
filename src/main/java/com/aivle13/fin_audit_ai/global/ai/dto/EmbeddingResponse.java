package com.aivle13.fin_audit_ai.global.ai.dto;

public record EmbeddingResponse(
        float[] embedding,
        String model
) {
}
