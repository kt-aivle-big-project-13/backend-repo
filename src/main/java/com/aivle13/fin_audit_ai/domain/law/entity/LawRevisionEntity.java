package com.aivle13.fin_audit_ai.domain.law.entity;

import com.aivle13.fin_audit_ai.domain.law.type.RevisionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 외부 기관(law.go.kr, msit, fsc)에서 수집한 법령 개정 감지 피드.
 * law_articles 와 FK 관계 없음 — 감지 시점엔 어느 조항인지 미확정이므로 독립 테이블.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "law_revisions")
public class LawRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revision_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "revision_type", nullable = false, length = 20)
    private RevisionType revisionType;

    @Column(name = "revised_at", nullable = false)
    private LocalDate revisedAt;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;
}
