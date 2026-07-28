package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 감사 중 수행하는 자율점검 항목별 답변.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "self_check_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_self_check_answers_audit_item",
                columnNames = {"audit_id", "item_code"}))
public class SelfCheckAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_code", nullable = false, length = 30)
    private SelfCheckItemCode itemCode;

    @Column(nullable = false)
    private boolean answer;

    public static SelfCheckAnswerEntity of(AuditEntity audit, SelfCheckItemCode itemCode, boolean answer) {
        SelfCheckAnswerEntity entity = new SelfCheckAnswerEntity();
        entity.audit = audit;
        entity.itemCode = itemCode;
        entity.answer = answer;
        return entity;
    }
}
