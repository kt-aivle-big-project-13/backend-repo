package com.aivle13.fin_audit_ai.domain.file.validator;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AuditFileValidator {
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024; // 200MB

    public void validateModelFile(MultipartFile file) {
        requireNotEmpty(file, "모델 파일");
        requireExtension(file, ".json");
        requireSizeLimit(file);
    }

    public void validateCsvFile(MultipartFile file, String label) {
        requireNotEmpty(file, label);
        requireExtension(file, ".csv");
        requireSizeLimit(file);
    }

    private void requireNotEmpty(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(label + " 파일이 존재하지 않습니다.");
        }
    }

    private void requireExtension(MultipartFile file, String extension) {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 확장자: " + name);
        }
    }

    private void requireSizeLimit(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 용량 초과: " + file.getOriginalFilename());
        }
    }
}
