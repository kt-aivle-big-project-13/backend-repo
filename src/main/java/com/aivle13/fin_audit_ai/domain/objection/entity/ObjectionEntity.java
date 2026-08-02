package com.aivle13.fin_audit_ai.domain.objection.entity;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionStatus;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * AI 모델의 자동심사 결과에 대한 고객 이의신청 처리 건.
 * 고객사 담당자가 신용감사 결과 CSV를 업로드하면 DRAFT 상태로 일괄 생성되고,
 * 담당자가 처리 결과를 확정해 고객 안내문을 발송하면 DELIVERED로 전환된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "objections")
public class ObjectionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objection_id")
    private Long id;

    @Column(name = "objection_no", nullable = false, unique = true, length = 20)
    private String objectionNo;

    // 이의제기가 어떤 AI 모델에 대한 것인지. CSV에는 모델 정보가 없어 optional로 둔다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private AiModelEntity model;

    @Column(name = "customer_name", nullable = false, length = 50)
    private String customerName;

    // 거절 금융기준
    @Column(name = "case_type", nullable = false, length = 100)
    private String caseType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 주요 판단 근거 변수 (CSV 원문 텍스트, 세미콜론 구분)
    @Column(name = "shap_evidence", columnDefinition = "TEXT")
    private String shapEvidence;

    // 담당자 판단 근거
    @Column(name = "staff_note", columnDefinition = "TEXT")
    private String staffNote;

    // 고객이 이의를 제기한 원 작성일시 (CSV 값 그대로)
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ObjectionDecision decision;

    @Column(name = "draft_content", columnDefinition = "TEXT")
    private String draftContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObjectionStatus status;

    // 승인자 (승인 전 NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private UserEntity approver;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // 고객 안내문 발송 일시
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // 발송 시점에 담당자가 직접 입력하는 수신 이메일 (발송 전에는 NULL)
    @Column(name = "recipient_email", length = 100)
    private String recipientEmail;

    public static ObjectionEntity create(
            String objectionNo,
            AiModelEntity model,
            String customerName,
            String caseType,
            String title,
            String content,
            String shapEvidence,
            String staffNote,
            LocalDateTime submittedAt
    ) {
        ObjectionEntity objection = new ObjectionEntity();
        objection.objectionNo = objectionNo;
        objection.model = model;
        objection.customerName = customerName;
        objection.caseType = caseType;
        objection.title = title;
        objection.content = content;
        objection.shapEvidence = shapEvidence;
        objection.staffNote = staffNote;
        objection.submittedAt = submittedAt;
        objection.status = ObjectionStatus.DRAFT;
        return objection;
    }

    // 발송 전 안내문 초안을 기록한다.
    public void recordDraft(String draftContent) {
        this.draftContent = Objects.requireNonNull(draftContent, "draftContent must not be null");
    }

    public void approve(UserEntity approver, ObjectionDecision decision, LocalDateTime approvedAt) {
        if (status != ObjectionStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태에서만 승인할 수 있습니다. 현재 상태: " + status);
        }
        this.approver = Objects.requireNonNull(approver, "approver must not be null");
        this.decision = Objects.requireNonNull(decision, "decision must not be null");
        this.approvedAt = Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        this.status = ObjectionStatus.APPROVED;
    }

    public void deliver(LocalDateTime deliveredAt, String recipientEmail) {
        if (status != ObjectionStatus.APPROVED) {
            throw new IllegalStateException("APPROVED 상태에서만 전달할 수 있습니다. 현재 상태: " + status);
        }
        this.deliveredAt = Objects.requireNonNull(deliveredAt, "deliveredAt must not be null");
        this.recipientEmail = Objects.requireNonNull(recipientEmail, "recipientEmail must not be null");
        this.status = ObjectionStatus.DELIVERED;
    }
}