package com.aivle13.fin_audit_ai.domain.model.dto.request;

import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ModelUploadRequest(
        MultipartFile file,

        @NotBlank String modelName,
        @NotNull ModelType modelType,

        // 미전달 시 "1.0.0"으로 대체
        String version,

        // 미전달 시 CREDIT_SCORING으로 대체
        ModelDomain domain
) {
}
