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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportPersistenceService {

    private final AuditRepository auditRepository;
    private final ReportRepository reportRepository;
    private final FileStorageService fileStorageService;

    // LLM 호출 및 S3 업로드 전에 감사 존재 여부를 확인한다.
    @Transactional(readOnly = true)
    public void validateAuditExists(Long auditId) {
        findAudit(auditId);
    }

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

        AuditEntity audit = findAudit(auditId);

        ReportEntity report = ReportEntity.create(
                audit,
                reportType,
                format,
                s3Key
        );

        return reportRepository.save(report).getId();
    }

    // 여러 포맷의 보고서를 하나의 트랜잭션으로 저장한다.
    // 하나라도 저장에 실패하면 모든 DB 저장을 롤백하고 S3 파일도 정리한다.
    @Transactional
    public Map<ReportFormat, Long> saveAll(
            Long auditId,
            ReportType reportType,
            Map<ReportFormat, String> storedFiles
    ) {
        List<String> s3Keys = List.copyOf(storedFiles.values());

        fileStorageService.deleteOnRollback(s3Keys);

        AuditEntity audit = findAudit(auditId);

        List<ReportEntity> reports = storedFiles.entrySet()
                .stream()
                .map(entry ->
                        ReportEntity.create(
                                audit,
                                reportType,
                                entry.getKey(),
                                entry.getValue()
                        )
                )
                .toList();

        List<ReportEntity> savedReports = reportRepository.saveAll(reports);

        Map<ReportFormat, Long> savedReportIds = new LinkedHashMap<>();

        for (ReportEntity savedReport : savedReports) {
            savedReportIds.put(
                    savedReport.getFormat(),
                    savedReport.getId()
            );
        }

        return savedReportIds;
    }

    // 감사 조회와 AUDIT_NOT_FOUND 예외 처리를 공통으로 사용한다.
    private AuditEntity findAudit(Long auditId) {
        return auditRepository.findById(auditId).orElseThrow(() -> new BusinessException(ErrorCode.AUDIT_NOT_FOUND));
    }
}