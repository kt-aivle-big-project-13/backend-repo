package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 모델에 대한 감사(XAI/공정성 검증) 진행 건.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "audits")
public class AuditEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id")
    private AiModelEntity model;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "audit_name", nullable = false, length = 100)
    private String auditName;

    // 감사 데이터 상대경로 (원본 미저장, 경로 참조)
    @Column(name = "dataset_path", nullable = false, length = 255)
    private String datasetPath;

    // 검증 데이터 상대경로 (선택 업로드이므로 nullable)
    @Column(name = "validation_dataset_path", length = 255)
    private String validationDatasetPath;

    // 민감변수 목록 (콤마 구분)
    @Column(name = "sensitive_features", nullable = false, length = 255)
    private String sensitiveFeatures;

    @Column(name = "current_step", nullable = false)
    private int currentStep = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 완료일 + 5년 (시행령 27조②)
    @Column(name = "retention_until")
    private LocalDate retentionUntil;

    public static AuditEntity create(AiModelEntity model, UserEntity user, String auditName, String datasetPath,
                                      String validationDatasetPath, String sensitiveFeatures) {
        AuditEntity audit = new AuditEntity();
        audit.model = model;
        audit.user = user;
        audit.auditName = auditName;
        audit.datasetPath = datasetPath;
        audit.validationDatasetPath = validationDatasetPath;
        audit.sensitiveFeatures = sensitiveFeatures;
        audit.status = AuditStatus.IN_PROGRESS;
        return audit;
    }
}
