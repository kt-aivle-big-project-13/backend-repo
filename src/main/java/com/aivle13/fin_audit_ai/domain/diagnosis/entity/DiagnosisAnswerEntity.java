package com.aivle13.fin_audit_ai.domain.diagnosis.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사전진단 문항별 답변 및 배점.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "diagnosis_answers")
public class DiagnosisAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnosis_id")
    private PreDiagnosisEntity diagnosis;

    @Column(name = "question_code", nullable = false, length = 30)
    private String questionCode;

    @Column(nullable = false)
    private boolean answer;

    @Column(nullable = false)
    private int score = 0;
}
