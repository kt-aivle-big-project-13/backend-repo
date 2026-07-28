package com.aivle13.fin_audit_ai.global.s3.service;

import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageServiceTest {

    private final List<String> deletedKeys = new ArrayList<>();
    private final List<String> failingKeys = new ArrayList<>();

    private final FileStorageService fileStorageService = new FileStorageService() {

        @Override
        public StoredFile store(MultipartFile file, String prefix) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StoredFile store(
                byte[] content,
                String originalFilename,
                String contentType,
                String prefix
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DownloadedFile download(String s3Key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String s3Key) {
            if (failingKeys.contains(s3Key)) {
                throw new RuntimeException("삭제 실패: " + s3Key);
            }

            deletedKeys.add(s3Key);
        }
    };

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void triggerCompletion(int status) {
        for (
                TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()
        ) {
            synchronization.afterCompletion(status);
        }
    }

    @Test
    void 롤백되면_등록된_키를_모두_삭제한다() {
        fileStorageService.deleteOnRollback(
                List.of("models/a.json", "datasets/b.csv")
        );

        triggerCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertThat(deletedKeys)
                .containsExactly("models/a.json", "datasets/b.csv");
    }

    @Test
    void 커밋되면_삭제하지_않는다() {
        fileStorageService.deleteOnRollback(
                List.of("models/a.json")
        );

        triggerCompletion(TransactionSynchronization.STATUS_COMMITTED);

        assertThat(deletedKeys).isEmpty();
    }

    @Test
    void 일부_키_삭제가_실패해도_나머지_키_삭제를_계속한다() {
        failingKeys.add("models/a.json");

        fileStorageService.deleteOnRollback(
                List.of("models/a.json", "datasets/b.csv")
        );

        triggerCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertThat(deletedKeys)
                .containsExactly("datasets/b.csv");
    }
}