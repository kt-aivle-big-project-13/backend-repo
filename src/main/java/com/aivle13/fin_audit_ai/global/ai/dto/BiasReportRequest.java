package com.aivle13.fin_audit_ai.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record BiasReportRequest(

        @JsonProperty("audit_id")
        Long auditId,

        @JsonProperty("model_s3_key")
        String modelS3Key,

        @JsonProperty("audit_dataset_s3_key")
        String auditDatasetS3Key,

        @JsonProperty("validation_dataset_s3_key")
        String validationDatasetS3Key,

        @JsonProperty("audit_name")
        String auditName,

        @JsonProperty("target_approval_rate")
        BigDecimal targetApprovalRate,

        @JsonProperty("manual_threshold")
        BigDecimal manualThreshold,

        @JsonProperty("sensitive_features")
        List<String> sensitiveFeatures
) {
}
