package com.aivle13.fin_audit_ai.domain.audit.dto;

import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AuditUploadRequestDto {
    private Long assessmentId;

    private MultipartFile modelFile;
    private MultipartFile auditDatasetFile;
    private MultipartFile validationDatasetFile;

    private String auditName;
    private String modelName;
    private ModelType modelType;
    private Double targetApprovalRate;
    private Double threshold;

    // 콤마로 구분된 민감변수 목록
    private String sensitiveFeatures;
}
