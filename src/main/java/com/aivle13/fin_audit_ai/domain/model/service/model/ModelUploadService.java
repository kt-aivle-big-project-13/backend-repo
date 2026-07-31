package com.aivle13.fin_audit_ai.domain.model.service.model;

import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import com.aivle13.fin_audit_ai.domain.model.dto.request.model.ModelUploadRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.model.ModelUploadResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelUploadService {

    private final AuditFileValidator fileValidator;
    private final FileStorageService fileStorageService;
    private final AiModelService aiModelService;

    @Transactional
    public ModelUploadResponse upload(Long userId, ModelUploadRequest request) {
        fileValidator.validateModelArtifactFile(request.file());

        StoredFile stored = fileStorageService.store(request.file(), "models");
        fileStorageService.deleteOnRollback(List.of(stored.s3Key()));

        AiModelEntity aiModel = aiModelService.create(
                userId, request.modelName(), request.modelType(), request.domain(),
                stored.s3Key(), stored.originalName(), request.version(), request.previousModelId()
        );

        return new ModelUploadResponse(
                aiModel.getId(),
                aiModel.getModelName(),
                aiModel.getVersion(),
                stored.originalName(),
                aiModel.getCreatedAt()
        );
    }
}
