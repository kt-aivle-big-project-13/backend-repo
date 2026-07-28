package com.aivle13.fin_audit_ai.global.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiResponse(
        String id,
        String status,
        List<OutputItem> output
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OutputItem(
            String type,
            List<ContentItem> content
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentItem(
            String type,
            String text
    ) {
    }
}