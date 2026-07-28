package com.aivle13.fin_audit_ai.global.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenAiResponseRequest(
        String model,
        String instructions,
        String input,
        @JsonProperty("store")
        boolean store
) {
}