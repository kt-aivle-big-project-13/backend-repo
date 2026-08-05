package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.core.ThresholdMethod;
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

    // thresholdMethod=VALIDATION_DATASET일 때 임계값 보정에 쓰는 데이터셋. 감사 생성 시점에
    // 계열 내 최신 것으로 자동 선택되거나 사용자가 직접 고른 값으로 확정되어, 이후 Fairlearn
    // 단계에서 다시 조회하지 않고 이 값을 그대로 쓴다. 검증 데이터셋이 없거나 MANUAL이면 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_dataset_id")
    private DatasetEntity validationDataset;

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

    // 감사셋 전체 모델 판별 성능 (AI 서버 산출). 성능-공정성 트레이드오프 표시에 쓴다.
    // 감사셋에 한 클래스만 있으면 AUC 가 정의되지 않아 null.
    @Column(name = "model_auc", precision = 6, scale = 4)
    private BigDecimal modelAuc;

    @Column(name = "model_accuracy", precision = 6, scale = 4)
    private BigDecimal modelAccuracy;

    // 재시도 때마다 증가하는 실행 세대. 취소 직후 재시도했을 때, 취소되기 전 실행에서
    // 뒤늦게 도착하는 AI 콜백(느린 SHAP/Fairlearn 응답)이 지금 실행의 상태를 덮어쓰지
    // 않도록 이벤트·콜백에 실어 보내 대조하는 용도로 쓴다.
    // columnDefinition의 DEFAULT 0은 기존 행이 있는 테이블에 NOT NULL 컬럼을 추가할 때
    // 제약 위반을 막기 위한 것이다(DEFAULT 없이 추가하면 ddl-auto:update가 실패한다 —
    // 실제로 재현해서 확인함).
    @Column(name = "generation", nullable = false, columnDefinition = "integer default 0")
    private int generation = 0;

    public static AuditEntity create(AiModelEntity model, DatasetEntity dataset, UserEntity user, String auditName,
                                      String sensitiveFeatures, Long assessmentId, ThresholdMethod thresholdMethod,
                                      BigDecimal targetApprovalRate, BigDecimal manualThreshold,
                                      DatasetEntity validationDataset) {
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
        audit.validationDataset = validationDataset;
        audit.status = AuditStatus.PENDING;
        return audit;
    }
    // SHAP 실행 시작
    public void markInProgress() {
        this.status = AuditStatus.IN_PROGRESS;
    }
    // SHAP 완료 후 Fairlearn 단계 이동
    public void moveToStep(int step) {
        this.currentStep = step;
    }
    // 모든 분석 완료: 지표 판정으로 산출된 준수 상태로 전이하고 완료 시각을 기록
    public void complete(int step, AuditStatus verdict) {
        this.currentStep = step;
        this.status = verdict;
        this.completedAt = LocalDateTime.now();
    }
    // AI 서버 오류·타임아웃
    public void markFailed() {
        this.status = AuditStatus.FAILED;
    }
    // 대기·진행 중인 감사만 취소할 수 있다 (이미 끝난 감사는 취소 대상이 아님)
    public boolean isCancellable() {
        return status == AuditStatus.PENDING || status == AuditStatus.IN_PROGRESS;
    }
    public boolean isCancelled() {
        return status == AuditStatus.CANCELLED;
    }
    public void cancel() {
        this.status = AuditStatus.CANCELLED;
    }
    // 실패했거나 취소된 감사만 재시도할 수 있다 (진행 중·완료된 감사는 재시도 대상이 아님)
    public boolean isRetryable() {
        return status == AuditStatus.FAILED || status == AuditStatus.CANCELLED;
    }
    // 모델·데이터셋 등 참조는 그대로 두고, 분석 산출물(진행 단계·완료 시각·성능 지표)만 초기화해
    // 처음부터 다시 분석하도록 되돌린다.
    public void retry() {
        this.status = AuditStatus.PENDING;
        this.currentStep = 1;
        this.completedAt = null;
        this.modelAuc = null;
        this.modelAccuracy = null;
        this.generation++;
    }
    // 공정성 단계에서 AI가 준 모델 성능(AUC·정확도)을 기록. 값이 없으면 null 로 남는다.
    public void applyPerformance(BigDecimal modelAuc, BigDecimal modelAccuracy) {
        this.modelAuc = modelAuc;
        this.modelAccuracy = modelAccuracy;
    }
}
