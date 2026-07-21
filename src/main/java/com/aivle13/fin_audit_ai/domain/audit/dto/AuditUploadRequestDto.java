package com.aivle13.fin_audit_ai.domain.audit.dto;

import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AuditUploadRequestDto {
    private MultipartFile modelFile;
    private MultipartFile auditDatasetFile;
    private MultipartFile validationDatasetFile;

    @NotBlank
    private String auditName;

    @NotBlank
    private String modelName;

    @NotNull
    private ModelType modelType;

    private Double targetApprovalRate;
    private Double threshold;

    // 콤마로 구분된 민감변수 목록
    @NotBlank
    private String sensitiveFeatures;
}
