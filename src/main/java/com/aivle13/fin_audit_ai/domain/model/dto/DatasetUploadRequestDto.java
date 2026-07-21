package com.aivle13.fin_audit_ai.domain.model.dto;

import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class DatasetUploadRequestDto {
    // 미전달 시 CUSTOMER로 대체
    private DataSource dataSource;

    private MultipartFile datasetFile;
}
