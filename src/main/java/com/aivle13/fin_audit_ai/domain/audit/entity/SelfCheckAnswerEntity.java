package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckAnswerValue;
import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckItemCode;
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

    // 기존 컬럼(answer, boolean)은 예/아니오만 표현할 수 있어 "해당없음"을 못 담는다.
    // 컬럼 타입을 바꾸는 대신(운영 DB에서 boolean→varchar ALTER는 ddl-auto:update로
    // 안전하게 안 됨) 새 컬럼을 추가했다. 예전 answer 컬럼은 더 이상 안 쓴다.
    // columnDefinition의 DEFAULT 'NO'는 기존 행이 있는 테이블에 NOT NULL 컬럼을 추가할 때
    // 제약 위반을 막기 위한 임시값이고, SelfCheckItemCodeMigrationRunner가 옛 answer(boolean)
    // 값 기준으로 정확한 값으로 다시 채운다.
    @Enumerated(EnumType.STRING)
    @Column(name = "answer_value", nullable = false, length = 10,
            columnDefinition = "varchar(10) default 'NO'")
    private SelfCheckAnswerValue answer;

    public static SelfCheckAnswerEntity of(AuditEntity audit, SelfCheckItemCode itemCode, SelfCheckAnswerValue answer) {
        SelfCheckAnswerEntity entity = new SelfCheckAnswerEntity();
        entity.audit = audit;
        entity.itemCode = itemCode;
        entity.answer = answer;
        return entity;
    }

    public boolean isYes() {
        return answer == SelfCheckAnswerValue.YES;
    }

    public boolean isNo() {
        return answer == SelfCheckAnswerValue.NO;
    }

    public boolean isNa() {
        return answer == SelfCheckAnswerValue.NA;
    }
}
