package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.law.service.AuditLawMappingQueryService;
import com.aivle13.fin_audit_ai.domain.report.document.GeneratedReportFile;
import com.aivle13.fin_audit_ai.domain.report.document.ReportDocumentGenerator;
import com.aivle13.fin_audit_ai.domain.report.dto.ReportGenerationContext;
import com.aivle13.fin_audit_ai.domain.report.prompt.ReportOutputValidator;
import com.aivle13.fin_audit_ai.domain.report.prompt.ReportPromptBuilder;
import com.aivle13.fin_audit_ai.domain.report.prompt.ReportPromptTemplate;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import com.aivle13.fin_audit_ai.domain.report.dto.GeneratedReportResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
// ReportLlmClient와 AuditLawMappingQueryService의 실제 구현체가 모두 병합되어 항상 Bean으로 등록되면 이 조건을 제거한다.
// 현재는 미구현 의존성 때문에 애플리케이션 컨텍스트가 실패하는 것을 방지하기 위한 임시 처리다.
@ConditionalOnBean({ReportLlmClient.class, AuditLawMappingQueryService.class})
public class ReportGenerationService {

    private static final String REPORT_PREFIX = "reports";

    private final ReportLlmClient reportLlmClient;
    private final ReportGenerationContextLoader contextLoader;
    private final AuditLawMappingQueryService auditLawMappingQueryService;
    private final ReportDocumentGenerator reportDocumentGenerator;
    private final FileStorageService fileStorageService;
    private final ReportPersistenceService reportPersistenceService;

    // 선택된 포맷별로 보고서를 생성하되 LLM 본문은 한 번만 생성한다.
    public List<GeneratedReportResponse> generate(Long auditId, List<ReportFormat> formats) {
        if (formats == null || formats.isEmpty()) {
            return List.of();
        }

        if (auditLawMappingQueryService.hasPendingMappings(auditId)) {
            throw new IllegalStateException("검토되지 않은 법령 매핑이 존재하여 보고서를 생성할 수 없습니다.");
        }

        ReportGenerationContext context = contextLoader.load(auditId);

        String generatedContent = generateContent(context);

        return formats.stream()
                .distinct()
                .map(format ->
                        generateAndSave(
                                auditId,
                                generatedContent,
                                format
                        )
                )
                .toList();
    }

    // 생성된 동일 본문을 포맷별 문서로 변환하고 S3 및 DB에 저장한다.
    private GeneratedReportResponse generateAndSave(
            Long auditId,
            String generatedContent,
            ReportFormat format
    ) {
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

        Long reportId = reportPersistenceService.save(
                auditId,
                format,
                storedFile.s3Key()
        );

        return GeneratedReportResponse.completed(
                reportId,
                format
        );
    }

    // 보고서 입력 데이터를 기반으로 LLM 보고서 본문 생성
    public String generateContent(ReportGenerationContext context) {
        String systemPrompt = ReportPromptTemplate.buildSystemPrompt();

        String userPrompt = ReportPromptBuilder.build(context);

        String generatedContent = reportLlmClient.generate(systemPrompt, userPrompt);

        ReportOutputValidator.validate(generatedContent);

        return generatedContent;
    }
}