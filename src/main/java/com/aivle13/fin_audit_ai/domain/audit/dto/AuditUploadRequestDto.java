package com.aivle13.fin_audit_ai.domain.audit.dto;

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
public class AuditUploadRequestDto {
    private MultipartFile modelFile;
    private MultipartFile auditDatasetFile;
    private MultipartFile validationDatasetFile;

    private String auditName;
    private String modelName;
    private String modelType;
    private Double targetApprovalRate;
    private Double threshold;
}
