package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.aimodel.service.AiModelService;
import com.aivle13.fin_audit_ai.domain.audit.dto.AuditUploadRequestDto;
import com.aivle13.fin_audit_ai.domain.audit.dto.AuditUploadResponseDto;
import com.aivle13.fin_audit_ai.domain.audit.validator.ThresholdPolicyValidator;
import com.aivle13.fin_audit_ai.domain.file.dto.StoredFile;
import com.aivle13.fin_audit_ai.domain.file.service.FileStorageService;
import com.aivle13.fin_audit_ai.domain.file.validator.AuditFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditUploadService {

    private final AuditFileValidator fileValidator;
    private final ThresholdPolicyValidator thresholdValidator;
    private final FileStorageService fileStorageService;
    private final AiModelService aiModelService;
    private final AuditService auditService;

    @Transactional
    public AuditUploadResponseDto upload(Long userId, AuditUploadRequestDto request) {
        // 1. 필수 파일 검증
        fileValidator.validateModelFile(request.getModelFile());
        fileValidator.validateCsvFile(request.getAuditDatasetFile(), "감사 데이터");

        // 2. 조건부 값(targetApprovalRate, threshold) 범위 검증
        thresholdValidator.validate(request);

        // 3. 필수 파일 S3 저장
        StoredFile modelStored = fileStorageService.store(request.getModelFile(), "models");
        StoredFile datasetStored = fileStorageService.store(request.getAuditDatasetFile(), "datasets");

        // 4. 선택 파일(검증 데이터) — 실패해도 무시하고 기본 감사 진행
        boolean validationUsable = false;
        if (isPresent(request.getValidationDatasetFile())) {
            try {
                fileValidator.validateCsvFile(request.getValidationDatasetFile(), "검증 데이터");
                fileStorageService.store(request.getValidationDatasetFile(), "datasets");
                validationUsable = true;
            } catch (RuntimeException e) {
                log.warn("검증 데이터 사용 불가, 기본 감사만 진행: {}", e.getMessage());
            }
        }

        // 5. AiModel 저장
        AiModel aiModel = aiModelService.create(
                userId, request.getModelName(), request.getModelType(), modelStored.s3Key()
        );

        // 6. Audit 생성
        Audit audit = auditService.create(userId, aiModel, datasetStored.s3Key());

        // 7. 응답 조립
        return new AuditUploadResponseDto(
                audit.getId(),
                aiModel.getId(),
                new AuditUploadResponseDto.UploadedFiles(true, true, validationUsable),
                audit.getStatus().name()
        );
    }

    private boolean isPresent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }
}
