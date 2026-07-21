package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.UploadedFiles;
import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditUploadRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditUploadResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditFileEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditFileRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.FileRole;
import com.aivle13.fin_audit_ai.domain.audit.validator.ThresholdPolicyValidator;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.service.AiModelService;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditUploadService {

    private final AuditFileValidator fileValidator;
    private final ThresholdPolicyValidator thresholdValidator;
    private final FileStorageService fileStorageService;
    private final AiModelService aiModelService;
    private final AuditService auditService;
    private final AuditFileRepository auditFileRepository;

    private record PendingFile(FileRole role, StoredFile file) {}

    @Transactional
    public AuditUploadResponse upload(Long userId, AuditUploadRequest request) {
        // 1. 필수 파일 검증
        fileValidator.validateModelFile(request.modelFile());
        fileValidator.validateCsvFile(request.auditDatasetFile(), "감사 데이터");

        // 2. 조건부 값(targetApprovalRate, threshold) 범위 검증
        thresholdValidator.validate(request);

        List<String> storedKeys = new ArrayList<>();
        List<PendingFile> pendingFiles = new ArrayList<>();
        fileStorageService.deleteOnRollback(storedKeys);

        // 3. 필수 파일 S3 저장
        StoredFile modelStored = fileStorageService.store(request.modelFile(), "models");
        storedKeys.add(modelStored.s3Key());
        pendingFiles.add(new PendingFile(FileRole.MODEL, modelStored));

        StoredFile datasetStored = fileStorageService.store(request.auditDatasetFile(), "datasets");
        storedKeys.add(datasetStored.s3Key());
        pendingFiles.add(new PendingFile(FileRole.AUDIT_DATASET, datasetStored));

        // 4. 선택 파일(검증 데이터) — 실패해도 무시하고 기본 감사 진행
        String validationDatasetKey = null;
        if (isPresent(request.validationDatasetFile())) {
            StoredFile validationStored = tryStoreValidationDataset(request.validationDatasetFile());
            if (validationStored != null) {
                storedKeys.add(validationStored.s3Key());
                pendingFiles.add(new PendingFile(FileRole.VALIDATION_DATASET, validationStored));
                validationDatasetKey = validationStored.s3Key();
            }
        }

        // 5. AiModel 저장
        AiModelEntity aiModel = aiModelService.create(
                userId, request.modelName(), request.modelType(), null, modelStored.s3Key(), null
        );

        // 6. Audit 생성
        AuditEntity audit = auditService.create(
                userId, aiModel, request.auditName(), datasetStored.s3Key(), validationDatasetKey, request.sensitiveFeatures()
        );

        // 6-1. 업로드된 파일 메타데이터 저장
        for (PendingFile pending : pendingFiles) {
            StoredFile file = pending.file();
            auditFileRepository.save(AuditFileEntity.create(
                    audit, pending.role(), file.s3Key(), file.originalName(), file.contentType(), file.size()
            ));
        }

        // 7. 응답 조립
        return new AuditUploadResponse(
                audit.getId(),
                aiModel.getId(),
                new UploadedFiles(true, true, validationDatasetKey != null),
                audit.getStatus().name()
        );
    }

    private StoredFile tryStoreValidationDataset(MultipartFile file) {
        try {
            fileValidator.validateCsvFile(file, "검증 데이터");
            return fileStorageService.store(file, "datasets");
        } catch (RuntimeException e) {
            log.warn("검증 데이터 사용 불가, 기본 감사만 진행: {}", e.getMessage());
            return null;
        }
    }

    private boolean isPresent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }
}
