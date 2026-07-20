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

/**
 * AI 모델의 자동심사 결과에 대한 고객 이의신청 처리 건.
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id")
    private AiModelEntity model;

    // 은행 내부 고객 참조번호 (개인정보 아님)
    @Column(name = "customer_ref", nullable = false, length = 50)
    private String customerRef;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ObjectionDecision decision;

    // SHAP 근거 변수 (JSON 문자열)
    @Column(name = "shap_evidence", columnDefinition = "TEXT")
    private String shapEvidence;

    @Column(name = "staff_note", columnDefinition = "TEXT")
    private String staffNote;

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

    // 승인 문서 은행 시스템 전달 일시
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    public static ObjectionEntity create(String objectionNo, AiModelEntity model, String customerRef) {
        ObjectionEntity objection = new ObjectionEntity();
        objection.objectionNo = objectionNo;
        objection.model = model;
        objection.customerRef = customerRef;
        objection.status = ObjectionStatus.DRAFT;
        return objection;
    }

    public void approve(UserEntity approver, ObjectionDecision decision, LocalDateTime approvedAt) {
        this.approver = approver;
        this.decision = decision;
        this.approvedAt = approvedAt;
        this.status = ObjectionStatus.APPROVED;
    }

    public void deliver(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
        this.status = ObjectionStatus.DELIVERED;
    }
}
