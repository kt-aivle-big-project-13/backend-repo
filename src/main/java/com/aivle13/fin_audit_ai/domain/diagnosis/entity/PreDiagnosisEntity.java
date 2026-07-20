package com.aivle13.fin_audit_ai.domain.diagnosis.entity;

import com.aivle13.fin_audit_ai.domain.diagnosis.type.DiagnosisResult;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import jakarta.persistence.*;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 모델의 고영향(high-impact) 해당 여부를 판별하는 사전진단 결과.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pre_diagnoses")
public class PreDiagnosisEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diagnosis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id")
    private AiModelEntity model;

    @Column(name = "condition_met", nullable = false)
    private boolean conditionMet = false;

    @Column(name = "group_a_score", nullable = false)
    private int groupAScore = 0;

    @Column(name = "group_b_score", nullable = false)
    private int groupBScore = 0;

    @Column(name = "total_score", nullable = false)
    private int totalScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiagnosisResult result;

    @Column(name = "report_path", length = 255)
    private String reportPath;
}
