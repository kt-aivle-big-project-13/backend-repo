package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExplainabilityReportRequest(

        @JsonProperty("audit_id")
        Long auditId,

        @JsonProperty("model_s3_key")
        String modelS3Key,

        @JsonProperty("audit_dataset_s3_key")
        String auditDatasetS3Key,

        @JsonProperty("target_column")
        String targetColumn,

        @JsonProperty("sensitive_features")
        List<String> sensitiveFeatures,

        @JsonProperty("report_top_n")
        Integer reportTopN
) {
}
