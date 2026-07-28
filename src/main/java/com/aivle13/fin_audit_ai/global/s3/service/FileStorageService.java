package com.aivle13.fin_audit_ai.global.s3.service;

import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    Logger LOG = LoggerFactory.getLogger(FileStorageService.class);

    StoredFile store(MultipartFile file, String prefix);

    // PDF/WORD 파일 저장
    StoredFile store(
            byte[] content,
            String originalFilename,
            String contentType,
            String prefix
    );

    DownloadedFile download(String s3Key);

    void delete(String s3Key);

    // 커밋 실패 등 메서드 반환 이후에 트랜잭션이 롤백되는 경우까지 포함해 S3 객체를 정리한다.
    default void deleteOnRollback(List<String> s3Keys) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    for (String s3Key : s3Keys) {
                        try {
                            delete(s3Key);
                        } catch (RuntimeException e) {
                            LOG.warn("S3 객체 정리 실패: key={}", s3Key, e);
                        }
                    }
                }
            }
        });
    }
}