package com.aivle13.fin_audit_ai.domain.model.dto.request;

import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ModelUploadRequestDto {
    private MultipartFile file;

    @NotBlank
    private String modelName;

    @NotNull
    private ModelType modelType;

    // 미전달 시 "1.0.0"으로 대체
    private String version;

    // 미전달 시 CREDIT_SCORING으로 대체
    private ModelDomain domain;
}
