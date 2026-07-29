package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportPersistenceService {

    private final AuditRepository auditRepository;
    private final ReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    // 감사 조회와 ReportEntity 저장에 필요한 DB 작업만 짧은 트랜잭션으로 처리한다.
    // 저장 실패로 트랜잭션이 롤백되면 업로드된 S3 파일도 함께 삭제한다.
    @Transactional
    public Long save(
            Long auditId,
            ReportType reportType,
            ReportFormat format,
            String s3Key
    ) {
        fileStorageService.deleteOnRollback(
                List.of(s3Key)
        );

        AuditEntity audit = auditRepository.findById(auditId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.AUDIT_NOT_FOUND
                        )
                );

        ReportEntity report = ReportEntity.create(
                audit,
                reportType,
                format,
                s3Key
        );

        return reportRepository.save(report).getId();
    }
}
