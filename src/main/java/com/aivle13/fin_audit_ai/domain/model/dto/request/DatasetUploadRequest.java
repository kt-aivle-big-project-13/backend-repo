package com.aivle13.fin_audit_ai.domain.model.dto.request;

import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import org.springframework.web.multipart.MultipartFile;

public record DatasetUploadRequest(
        // 미전달 시 CUSTOMER로 대체
        DataSource dataSource,

        // 미전달 시 AUDIT로 대체
        DatasetPurpose purpose,

        MultipartFile datasetFile
) {
}
