package com.aivle13.fin_audit_ai.domain.report.service;

import com.aivle13.fin_audit_ai.domain.report.document.GeneratedReportFile;
import com.aivle13.fin_audit_ai.domain.report.document.ReportDocumentGenerator;
import com.aivle13.fin_audit_ai.domain.report.dto.GeneratedReportResponse;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final String REPORT_PREFIX = "reports";

    private final ReportLlmClient reportLlmClient;
    private final ReportGenerationContextLoader contextLoader;
    private final ReportDocumentGenerator reportDocumentGenerator;
    private final FileStorageService fileStorageService;
    private final ReportPersistenceService reportPersistenceService;

    // 선택된 포맷별로 최종 감사 보고서를 생성한다.
    // 여러 포맷을 선택해도 LLM 본문은 한 번만 생성한다.
    public List<GeneratedReportResponse> generate(
            Long auditId,
            List<ReportFormat> formats
    ) {
        if (formats == null || formats.isEmpty()) {
            return List.of();
        }

        ReportGenerationContext context =
                contextLoader.load(auditId);

        // 설명 가능성 + 편향 진단 + 규제 준수 + 개선 권고를 포함한 최종 통합 보고서 본문을 한 번만 생성한다.
        String generatedContent =
                generateContent(context);

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

    // 같은 최종 통합 보고서 본문을 PDF 또는 Word로 변환하고 S3 및 DB에 각각 저장한다.
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

        StoredFile storedFile =
                fileStorageService.store(
                        generatedFile.content(),
                        generatedFile.fileName(),
                        generatedFile.contentType(),
                        REPORT_PREFIX
                );

        Long reportId =
                reportPersistenceService.save(
                        auditId,
                        format,
                        storedFile.s3Key()
                );

        return GeneratedReportResponse.completed(
                reportId,
                format
        );
    }

    // 보고서 입력 데이터를 기반으로 최종 통합 보고서 본문 생성
    public String generateContent(ReportGenerationContext context) {
        String systemPrompt =
                ReportPromptTemplate.buildSystemPrompt();

        String userPrompt =
                ReportPromptBuilder.build(context);

        String generatedContent =
                reportLlmClient.generate(
                        systemPrompt,
                        userPrompt
                );

        ReportOutputValidator.validate(generatedContent);

        return generatedContent;
    }
}