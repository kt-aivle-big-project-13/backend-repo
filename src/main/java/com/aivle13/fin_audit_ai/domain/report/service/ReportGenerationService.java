package com.aivle13.fin_audit_ai.domain.report.service;

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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final String REPORT_PREFIX = "reports";

    private final ReportLlmClient reportLlmClient;
    private final ReportGenerationContextLoader contextLoader;
    private final ReportDocumentGenerator reportDocumentGenerator;
    private final FileStorageService fileStorageService;
    private final ReportPersistenceService reportPersistenceService;

    // LLM 호출, 문서 생성, S3 업로드는 장시간 소요될 수 있으므로 DB 트랜잭션 밖에서 수행한다.
    // ReportEntity 저장만 ReportPersistenceService의 별도 트랜잭션에서 처리한다.
    public Long generate(Long auditId, ReportFormat format) {
        ReportGenerationContext context =
                contextLoader.load(auditId);

        String generatedContent =
                generateContent(context);

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

        return reportPersistenceService.save(
                auditId,
                format,
                storedFile.s3Key()
        );
    }

    // 보고서 입력 데이터를 기반으로 LLM 보고서 본문 생성
    public String generateContent(
            ReportGenerationContext context
    ) {
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