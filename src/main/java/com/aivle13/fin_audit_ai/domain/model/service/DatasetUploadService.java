package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.dto.request.DatasetUploadRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.DatasetUploadResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.config.CacheConfig;
import com.aivle13.fin_audit_ai.global.exception.file.InvalidFileFormatException;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetUploadService {

    private final AuditFileValidator fileValidator;
    private final FileStorageService fileStorageService;
    private final AiModelRepository aiModelRepository;
    private final DatasetRepository datasetRepository;
    private final CacheManager cacheManager;

    private record CsvSummary(List<String> columns, int rowCount) {}

    @Transactional
    public DatasetUploadResponse upload(Long userId, Long modelId, DatasetUploadRequest request) {
        AiModelEntity model = aiModelRepository.findByIdAndUser_Id(modelId, userId)
                .orElseThrow(ModelNotFoundException::new);

        DataSource dataSource = request.dataSource() != null ? request.dataSource() : DataSource.CUSTOMER;
        DatasetPurpose purpose = request.purpose() != null ? request.purpose() : DatasetPurpose.AUDIT;

        fileValidator.validateCsvFile(request.datasetFile(), "감사 데이터셋");

        CsvSummary summary = readCsv(request.datasetFile());

        List<String> storedKeys = new ArrayList<>();
        registerCleanupOnRollback(storedKeys);

        StoredFile stored = fileStorageService.store(request.datasetFile(), "datasets");
        storedKeys.add(stored.s3Key());

        DatasetEntity dataset = DatasetEntity.create(
                model, dataSource, stored.s3Key(), summary.rowCount(), String.join(",", summary.columns())
        );
        if (purpose == DatasetPurpose.VALIDATION) {
            dataset.markAsValidation();
        }
        datasetRepository.save(dataset);

        evictCache(userId, model.getModelGroupId(), dataset.getPurpose());

        return new DatasetUploadResponse(
                dataset.getId(), model.getId(), dataset.getDataSource().name(),
                dataset.getPurpose().name(), dataset.getRowCount(), summary.columns()
        );
    }

    // DatasetQueryService.listByModelGroup() 캐시 키가 (userId, modelGroupId, purpose)이므로,
    // 방금 저장한 purpose로 조회한 목록과 purpose 없이 전체 조회한 목록 두 캐시 엔트리만 무효화한다.
    private void evictCache(Long userId, String modelGroupId, DatasetPurpose purpose) {
        Cache cache = cacheManager.getCache(CacheConfig.DATASETS_CACHE);

        if (cache == null) {
            return;
        }

        cache.evict(userId + ":" + modelGroupId + ":" + purpose);
        cache.evict(userId + ":" + modelGroupId + ":" + null);
    }

    private CsvSummary readCsv(MultipartFile file) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            List<String> columns = new ArrayList<>(parser.getHeaderNames());
            int rowCount = 0;
            for (CSVRecord ignored : parser) {
                rowCount++;
            }
            return new CsvSummary(columns, rowCount);
        } catch (IOException e) {
            throw new InvalidFileFormatException("CSV 파일을 읽을 수 없습니다: " + file.getOriginalFilename());
        }
    }

    // 커밋 실패 등 메서드 반환 이후에 트랜잭션이 롤백되는 경우까지 포함해 S3 객체를 정리한다.
    private void registerCleanupOnRollback(List<String> s3Keys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    for (String s3Key : s3Keys) {
                        try {
                            fileStorageService.delete(s3Key);
                        } catch (RuntimeException e) {
                            log.warn("S3 객체 정리 실패: key={}", s3Key, e);
                        }
                    }
                }
            }
        });
    }
}
