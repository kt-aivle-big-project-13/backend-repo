package com.aivle13.fin_audit_ai.domain.file.service;

import com.aivle13.fin_audit_ai.domain.file.dto.StoredFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile store(MultipartFile file, String prefix);
    void delete(String s3Key);
}
