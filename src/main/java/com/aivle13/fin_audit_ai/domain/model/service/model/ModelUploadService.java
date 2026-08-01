package com.aivle13.fin_audit_ai.domain.model.service.model;

import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import com.aivle13.fin_audit_ai.domain.model.dto.request.model.ModelUploadRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.model.ModelUploadResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidModelFileException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelUploadService {

    private final AuditFileValidator fileValidator;
    private final FileStorageService fileStorageService;
    private final AiModelService aiModelService;
    private final AiModelRepository aiModelRepository;

    @Transactional
    public ModelUploadResponse upload(Long userId, ModelUploadRequest request) {
        ArtifactSource artifact = resolveArtifact(userId, request.file(), request.previousModelId());

        AiModelEntity aiModel = aiModelService.create(
                userId, request.modelName(), request.modelType(), request.domain(),
                artifact.artifactPath(), artifact.originalFileName(), request.version(), request.previousModelId()
        );

        return new ModelUploadResponse(
                aiModel.getId(),
                aiModel.getModelName(),
                aiModel.getVersion(),
                artifact.originalFileName(),
                aiModel.getCreatedAt()
        );
    }

    private record ArtifactSource(String artifactPath, String originalFileName) {}

    // 새 파일을 올렸으면 그걸 저장하고, 안 올렸으면 버전업 대상인 이전 모델의 파일을 그대로 재사용한다
    // (같은 모델 파일로 메타데이터만 새 버전으로 등록하고 싶은 경우).
    private ArtifactSource resolveArtifact(Long userId, MultipartFile file, Long previousModelId) {
        if (file != null && !file.isEmpty()) {
            fileValidator.validateModelArtifactFile(file);

            StoredFile stored = fileStorageService.store(file, "models");
            fileStorageService.deleteOnRollback(List.of(stored.s3Key()));

            return new ArtifactSource(stored.s3Key(), stored.originalName());
        }

        if (previousModelId == null) {
            throw new InvalidModelFileException("모델 파일을 업로드하거나 이전 모델을 지정해야 합니다.");
        }

        AiModelEntity previousModel = aiModelRepository.findByIdAndUser_Id(previousModelId, userId)
                .orElseThrow(ModelNotFoundException::new);

        return new ArtifactSource(previousModel.getArtifactPath(), previousModel.getOriginalFileName());
    }
}
