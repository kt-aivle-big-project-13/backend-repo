package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.law.service.AuditLawMappingQueryService;
import com.aivle13.fin_audit_ai.domain.report.document.GeneratedReportFile;
import com.aivle13.fin_audit_ai.domain.report.document.ReportDocumentGenerator;
import com.aivle13.fin_audit_ai.domain.report.dto.ReportGenerationContext;
import com.aivle13.fin_audit_ai.domain.report.entity.ReportEntity;
import com.aivle13.fin_audit_ai.domain.report.prompt.ReportOutputValidator;
import com.aivle13.fin_audit_ai.domain.report.prompt.ReportPromptBuilder;
import com.aivle13.fin_audit_ai.domain.report.prompt.ReportPromptTemplate;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final String REPORT_PREFIX = "reports";

    private final ReportLlmClient reportLlmClient;
    private final AuditRepository auditRepository;
    private final ReportRepository reportRepository;
    private final ReportGenerationContextLoader contextLoader;
    private final AuditLawMappingQueryService auditLawMappingQueryService;
    private final ReportDocumentGenerator reportDocumentGenerator;
    private final FileStorageService fileStorageService;

    // 최종 감사 보고서 생성 및 저장
    @Transactional
    public Long generate(Long auditId, ReportFormat format) {
        if (auditLawMappingQueryService.hasPendingMappings(auditId)) {
            throw new IllegalStateException(
                    "검토되지 않은 법령 매핑이 존재하여 보고서를 생성할 수 없습니다."
            );
        }

        AuditEntity audit = auditRepository.findById(auditId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "감사 정보를 찾을 수 없습니다. auditId=" + auditId
                        )
                );

        ReportGenerationContext context = contextLoader.load(auditId);

        String generatedContent = generateContent(context);

        GeneratedReportFile generatedFile =
                reportDocumentGenerator.generate(
                        auditId,
                        generatedContent,
                        format
                );

        StoredFile storedFile = fileStorageService.store(
                generatedFile.content(),
                generatedFile.fileName(),
                generatedFile.contentType(),
                REPORT_PREFIX
        );

        // DB 저장 실패로 트랜잭션이 롤백되면 S3 파일 삭제
        fileStorageService.deleteOnRollback(
                List.of(storedFile.s3Key())
        );

        ReportEntity report = ReportEntity.create(
                audit,
                ReportType.FINAL_AUDIT_REPORT,
                format,
                storedFile.s3Key()
        );

        return reportRepository.save(report).getId();
    }

    // 보고서 입력 데이터를 기반으로 LLM 보고서 본문 생성
    public String generateContent(ReportGenerationContext context) {
        String systemPrompt =
                ReportPromptTemplate.buildSystemPrompt();

        String userPrompt =
                ReportPromptBuilder.build(context);

        String generatedContent = reportLlmClient.generate(
                systemPrompt,
                userPrompt
        );

        ReportOutputValidator.validate(generatedContent);

        return generatedContent;
    }
}