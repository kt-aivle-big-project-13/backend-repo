package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import com.aivle13.fin_audit_ai.domain.model.dto.ModelUploadRequestDto;
import com.aivle13.fin_audit_ai.domain.model.dto.ModelUploadResponseDto;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelUploadService {

    private final AuditFileValidator fileValidator;
    private final FileStorageService fileStorageService;
    private final AiModelService aiModelService;

    @Transactional
    public ModelUploadResponseDto upload(Long userId, ModelUploadRequestDto request) {
        fileValidator.validateModelArtifactFile(request.getFile());

        StoredFile stored = fileStorageService.store(request.getFile(), "models");
        try {
            AiModelEntity aiModel = aiModelService.create(
                    userId, request.getModelName(), request.getModelType(),
                    request.getDomain(), stored.s3Key(), request.getVersion()
            );

            return new ModelUploadResponseDto(
                    aiModel.getId(),
                    aiModel.getModelName(),
                    aiModel.getVersion(),
                    stored.originalName(),
                    aiModel.getCreatedAt()
            );
        } catch (RuntimeException e) {
            try {
                fileStorageService.delete(stored.s3Key());
            } catch (RuntimeException cleanupEx) {
                log.warn("S3 객체 정리 실패: key={}", stored.s3Key(), cleanupEx);
            }
            throw e;
        }
    }
}
