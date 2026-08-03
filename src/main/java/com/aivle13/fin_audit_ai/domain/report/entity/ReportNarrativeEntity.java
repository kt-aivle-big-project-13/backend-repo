package com.aivle13.fin_audit_ai.domain.report.entity;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리포트 한 섹션의 서술 본문.
 *
 * <p>감사 질의 챗봇이 리포트 내용을 근거로 답하려면 본문 텍스트가 DB 에 있어야 한다.
 * 리포트 파일은 S3 에 HTML/PDF/DOCX 로만 있어, 저장해 두지 않으면 질문마다 파일을 받아
 * 파싱하게 된다.
 *
 * <p>서술은 포맷과 무관하므로 감사 + 리포트 종류 단위로 보관한다. 같은 리포트를 다시 만들면
 * 이전 서술은 지우고 새로 저장한다 — 과거 서술을 남겨두면 챗봇이 지금 리포트에 없는 문장을
 * 근거로 인용하게 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "report_narratives")
public class ReportNarrativeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "narrative_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @Column(name = "section_key", nullable = false, length = 50)
    private String sectionKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 목차 순서를 그대로 보존한다. 챗봇 프롬프트에 넣을 때 리포트와 같은 순서여야
    // 답변이 인용하는 "N장"이 실제 문서와 어긋나지 않는다.
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public static ReportNarrativeEntity create(
            AuditEntity audit,
            ReportType reportType,
            String sectionKey,
            String title,
            String content,
            int displayOrder
    ) {
        ReportNarrativeEntity narrative = new ReportNarrativeEntity();
        narrative.audit = audit;
        narrative.reportType = reportType;
        narrative.sectionKey = sectionKey;
        narrative.title = title;
        narrative.content = content;
        narrative.displayOrder = displayOrder;

        return narrative;
    }
}
