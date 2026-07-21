package com.aivle13.fin_audit_ai.global.s3.validator;

import com.aivle13.fin_audit_ai.global.exception.file.EmptyFileException;
import com.aivle13.fin_audit_ai.global.exception.file.InvalidFileFormatException;
import com.aivle13.fin_audit_ai.global.exception.model.FileSizeExceededException;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidModelFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AuditFileValidator {
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024; // 200MB

    public void validateModelFile(MultipartFile file) {
        requireNotEmpty(file, "모델 파일");
        requireModelExtension(file);
        requireSizeLimit(file);
    }

    public void validateCsvFile(MultipartFile file, String label) {
        requireNotEmpty(file, label);
        requireCsvExtension(file);
        requireSizeLimit(file);
    }

    private void requireNotEmpty(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException(label + " 파일이 존재하지 않습니다.");
        }
    }

    private void requireModelExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".json")) {
            throw new InvalidModelFileException("지원하지 않는 파일 확장자: " + name);
        }
    }

    private void requireCsvExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".csv")) {
            throw new InvalidFileFormatException("지원하지 않는 파일 확장자: " + name);
        }
    }

    private void requireSizeLimit(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeExceededException("파일 용량 초과: " + file.getOriginalFilename());
        }
    }
}
