package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import com.aivle13.fin_audit_ai.domain.model.dto.request.ModelUploadRequestDto;
import com.aivle13.fin_audit_ai.domain.model.dto.response.ModelUploadResponseDto;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        registerCleanupOnRollback(stored.s3Key());

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
    }

    // 커밋 실패 등 메서드 반환 이후에 트랜잭션이 롤백되는 경우까지 포함해 S3 객체를 정리한다.
    private void registerCleanupOnRollback(String s3Key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try {
                        fileStorageService.delete(s3Key);
                    } catch (RuntimeException cleanupEx) {
                        log.warn("S3 객체 정리 실패: key={}", s3Key, cleanupEx);
                    }
                }
            }
        });
    }
}
