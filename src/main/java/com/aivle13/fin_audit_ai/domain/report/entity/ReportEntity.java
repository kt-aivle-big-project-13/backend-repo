package com.aivle13.fin_audit_ai.domain.report.entity;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 감사 결과로 생성되는 보고서(XAI, 편향, 준수판정 등) 파일 메타데이터
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reports")
public class ReportEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id")
    private AuditEntity audit;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    @Column(nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    // 생성 완료된 보고서 파일 메타데이터 생성
    public static ReportEntity create(
            AuditEntity audit,
            ReportType reportType,
            ReportFormat format,
            String filePath
    ) {
        ReportEntity report = new ReportEntity();
        report.audit = audit;
        report.reportType = reportType;
        report.format = format;
        report.status = ReportStatus.COMPLETED;
        report.filePath = filePath;

        return report;
    }
}
