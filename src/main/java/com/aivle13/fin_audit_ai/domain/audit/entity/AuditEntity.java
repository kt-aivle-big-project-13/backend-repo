package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    // 데이터셋은 재업로드해도 새 row로 쌓이고 수정 API가 없어 FK 참조만으로 안전함
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id")
    private DatasetEntity dataset;

    // 고영향 AI 사전진단 건 ID (스킵 시 미전달, nullable). 사전진단 도메인은
    // 별도로 개발 중이라 FK로 엮지 않고 참조값만 보관한다.
    @Column(name = "assessment_id")
    private Long assessmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "audit_name", nullable = false, length = 100)
    private String auditName;

    // 감사 생성 시점 데이터셋의 민감정보 스냅샷(콤마 구분). 감사가 시작되면 데이터셋은
    // markAudited()로 잠겨 이후 수정이 막히지만, 이 감사가 실제로 사용한 값을 그대로
    // 복사해 데이터셋과 무관하게 불변으로 남긴다.
    @Column(name = "sensitive_features", nullable = false, length = 255)
    private String sensitiveFeatures;

    @Column(name = "current_step", nullable = false)
    private int currentStep = 1;

    // 승인·거절 기준값 산정 방식. AI 서버가 감사 실행 시점에 필요로 하는 정책값이라
    // 감사 시작과 함께 받아 그대로 보관한다 (Fairlearn 컬럼 매핑은 AI 파이프라인이 자체 처리).
    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_method", nullable = false, length = 20)
    private ThresholdMethod thresholdMethod;

    @Column(name = "target_approval_rate", precision = 5, scale = 4)
    private BigDecimal targetApprovalRate;

    @Column(name = "manual_threshold", precision = 5, scale = 4)
    private BigDecimal manualThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 완료일 + 5년 (시행령 27조②)
    @Column(name = "retention_until")
    private LocalDate retentionUntil;

    public static AuditEntity create(AiModelEntity model, DatasetEntity dataset, UserEntity user, String auditName,
                                      String sensitiveFeatures, Long assessmentId, ThresholdMethod thresholdMethod,
                                      BigDecimal targetApprovalRate, BigDecimal manualThreshold) {
        AuditEntity audit = new AuditEntity();
        audit.model = model;
        audit.dataset = dataset;
        audit.user = user;
        audit.auditName = auditName;
        audit.sensitiveFeatures = sensitiveFeatures;
        audit.assessmentId = assessmentId;
        audit.thresholdMethod = thresholdMethod;
        audit.targetApprovalRate = targetApprovalRate;
        audit.manualThreshold = manualThreshold;
        audit.status = AuditStatus.PENDING;
        return audit;
    }
}
