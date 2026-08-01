package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record ChatAnswerResponse(

        @JsonProperty("audit_id")
        Long auditId,

        String answer,

        List<Citation> citations,

        @JsonProperty("grounding_status")
        String groundingStatus,

        @JsonProperty("generated_at")
        String generatedAt
) {

    public record Citation(

            String type,

            String reference,

            String value,

            BigDecimal similarity,

            @JsonProperty("source_url")
            String sourceUrl,

            boolean revised
    ) {
    }
}
