package com.aivle13.fin_audit_ai.domain.model.dto.request;

import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import org.springframework.web.multipart.MultipartFile;

public record DatasetUploadRequest(
        // 미전달 시 CUSTOMER로 대체
        DataSource dataSource,

        MultipartFile datasetFile
) {
}
