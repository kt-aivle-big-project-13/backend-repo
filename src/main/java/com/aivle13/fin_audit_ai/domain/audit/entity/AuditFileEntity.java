package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.FileRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 감사 업로드 시 저장된 파일(모델/감사 데이터/검증 데이터)의 메타데이터.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "audit_files")
public class AuditFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_file_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_role", nullable = false, length = 20)
    private FileRole fileRole;

    @Column(name = "s3_key", nullable = false, length = 255)
    private String s3Key;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(nullable = false)
    private long size;

    public static AuditFileEntity create(AuditEntity audit, FileRole fileRole, String s3Key, String originalName, String contentType, long size) {
        AuditFileEntity file = new AuditFileEntity();
        file.audit = audit;
        file.fileRole = fileRole;
        file.s3Key = s3Key;
        file.originalName = originalName;
        file.contentType = contentType;
        file.size = size;
        return file;
    }
}
