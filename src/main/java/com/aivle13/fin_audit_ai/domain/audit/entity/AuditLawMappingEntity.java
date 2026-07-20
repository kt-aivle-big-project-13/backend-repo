package com.aivle13.fin_audit_ai.domain.audit.entity;

import com.aivle13.fin_audit_ai.domain.audit.type.ComplianceStatus;
import com.aivle13.fin_audit_ai.domain.law.entity.LawArticleEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 감사와 법령 조항 간 준수 여부 매핑 및 근거.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "audit_law_mappings")
public class AuditLawMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    private LawArticleEntity article;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplianceStatus compliance;

    @Column(columnDefinition = "TEXT")
    private String evidence;
}
