package com.aivle13.fin_audit_ai.global.s3.service;


import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.exception.file.FileUploadFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public StoredFile store(MultipartFile file, String prefix) {
        String key = "%s/%s_%s".formatted(
                prefix,
                UUID.randomUUID(),
                file.getOriginalFilename()
        );

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new FileUploadFailedException(file.getOriginalFilename(), e);
        }

        return new StoredFile(key, file.getOriginalFilename(), file.getContentType(), file.getSize());
    }

    @Override
    public void delete(String s3Key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .build()
        );
    }
}
