package com.aivle13.fin_audit_ai.domain.model.dto.request.model;

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
        ModelDomain domain,

        // 기존 모델의 새 버전으로 등록할 때만 사용. 지정하면 해당 모델의 계열(modelGroupId)을 이어받고,
        // 미전달 시 완전히 새로운 모델로 취급해 새 계열을 발급한다.
        Long previousModelId
) {
}
