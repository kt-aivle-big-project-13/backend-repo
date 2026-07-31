package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record HighImpactReportRequest(

        @JsonProperty("audit_id")
        Long auditId,

        @JsonProperty("assessment_id")
        Long assessmentId,

        @JsonProperty("audit_name")
        String auditName,

        @JsonProperty("model_name")
        String modelName,

        @JsonProperty("model_version")
        String modelVersion,

        @JsonProperty("assessed_at")
        String assessedAt,

        @JsonProperty("condition_met")
        boolean conditionMet,

        @JsonProperty("group_a_score")
        int groupAScore,

        @JsonProperty("group_b_score")
        int groupBScore,

        @JsonProperty("total_score")
        int totalScore,

        String result,

        List<Answer> answers
) {

    public record Answer(

            @JsonProperty("question_code")
            String questionCode,

            @JsonProperty("question_text")
            String questionText,

            String stage,

            String group,

            boolean answer,

            int weight,

            int score
    ) {
    }
}
