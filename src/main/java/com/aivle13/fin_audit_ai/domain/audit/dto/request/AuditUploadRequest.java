package com.aivle13.fin_audit_ai.domain.audit.dto.request;

import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import org.springframework.web.multipart.MultipartFile;

public record AuditUploadRequest(
        MultipartFile modelFile,
        MultipartFile auditDatasetFile,
        MultipartFile validationDatasetFile,

        String auditName,
        String modelName,
        ModelType modelType,
        Double targetApprovalRate,
        Double threshold,

        // 콤마로 구분된 민감변수 목록
        String sensitiveFeatures
) {
}
