package com.aivle13.fin_audit_ai.global.s3.service;


import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.exception.file.FileUploadFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
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
        String key = createKey(prefix, file.getOriginalFilename());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
            );
        } catch (IOException e) {
            throw new FileUploadFailedException(file.getOriginalFilename(), e);
        }

        return new StoredFile(key, file.getOriginalFilename(), file.getContentType(), file.getSize());
    }

    // PDF/WORD byte[] 파일 저장
    @Override
    public StoredFile store(
            byte[] content,
            String originalFilename,
            String contentType,
            String prefix
    ) {
        String key = createKey(prefix, originalFilename);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content)
            );
        } catch (RuntimeException e) {
            throw new FileUploadFailedException(
                    originalFilename,
                    e
            );
        }

        return new StoredFile(
                key,
                originalFilename,
                contentType,
                content.length
        );
    }

    @Override
    public DownloadedFile download(String s3Key) {
        ResponseInputStream<GetObjectResponse> object = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .build()
        );

        GetObjectResponse metadata = object.response();

        return new DownloadedFile(object, metadata.contentType(), metadata.contentLength());
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

    // S3 객체 키 생성
    private String createKey(
            String prefix,
            String originalFilename
    ) {
        return "%s/%s_%s".formatted(
                prefix,
                UUID.randomUUID(),
                originalFilename
        );
    }
}