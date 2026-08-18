package com.aivle13.fin_audit_ai.domain.report.service.common;

import com.aivle13.fin_audit_ai.domain.report.document.GeneratedReportFile;
import com.aivle13.fin_audit_ai.domain.report.document.ReportDocumentGenerator;
import com.aivle13.fin_audit_ai.domain.report.dto.response.common.GeneratedReportResponse;
import com.aivle13.fin_audit_ai.domain.report.dto.response.common.ReportGenerationContext;
import com.aivle13.fin_audit_ai.domain.report.prompt.ReportOutputValidator;
import com.aivle13.fin_audit_ai.domain.report.prompt.ReportPromptBuilder;
import com.aivle13.fin_audit_ai.domain.report.prompt.ReportPromptTemplate;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final String REPORT_PREFIX = "reports";

    private final ReportLlmClient reportLlmClient;
    private final ReportGenerationContextLoader contextLoader;
    private final ReportDocumentGenerator reportDocumentGenerator;
    private final FileStorageService fileStorageService;
    private final ReportPersistenceService reportPersistenceService;
    private final ReportGenerationGuard generationGuard;

    // 선택된 포맷별로 최종 감사 보고서를 생성한다.
    // 여러 포맷을 선택해도 LLM 본문은 한 번만 생성한다.
    public List<GeneratedReportResponse> generate(
            Long userId,
            Long auditId,
            List<ReportFormat> formats
    ) {
        if (formats == null || formats.isEmpty()) {
            return List.of();
        }

        // LLM 호출 및 S3 업로드 전에 요청자가 그 감사의 소유자인지 검증한다.
        reportPersistenceService.validateAuditOwnedBy(userId, auditId);

        List<ReportFormat> distinctFormats = formats.stream().distinct().toList();

        // 선생성과 사용자 요청이 겹치면 같은 보고서를 두 번 만들게 된다.
        // 이미 있으면 그것을 쓰고, 만드는 중이면 끝날 때까지 기다린다.
        return generationGuard.generateOnce(
                auditId,
                ReportType.FINAL_AUDIT_REPORT,
                () -> findExisting(userId, auditId, distinctFormats),
                () -> doGenerate(auditId, distinctFormats)
        );
    }

    /**
     * 요청한 포맷이 모두 이미 만들어져 있으면 그 결과. 하나라도 없으면 비어 있다.
     *
     * <p>일부만 있는 상태에서 그것만 돌려주면 요청한 포맷이 빠진 응답이 나가므로,
     * 그때는 전부 다시 만든다.
     */
    private Optional<List<GeneratedReportResponse>> findExisting(
            Long userId,
            Long auditId,
            List<ReportFormat> formats
    ) {
        List<GeneratedReportResponse> found = new ArrayList<>();

        for (ReportFormat format : formats) {
            Long reportId = generationGuard.findCompletedReportId(
                    userId,
                    auditId,
                    ReportType.FINAL_AUDIT_REPORT,
                    format
            );

            if (reportId == null) {
                return Optional.empty();
            }

            found.add(GeneratedReportResponse.completed(reportId, format));
        }

        return Optional.of(found);
    }

    private List<GeneratedReportResponse> doGenerate(
            Long auditId,
            List<ReportFormat> distinctFormats
    ) {
        ReportGenerationContext context = contextLoader.load(auditId);

        String generatedContent = generateContent(context);

        // 요청 순서를 유지하면서 포맷별 S3 key를 저장한다.
        Map<ReportFormat, String> storedFiles = new LinkedHashMap<>();

        try {
            for (ReportFormat format : distinctFormats) {
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

                storedFiles.put(
                        format,
                        storedFile.s3Key()
                );
            }
        } catch (RuntimeException exception) {
            // 이미 업로드된 S3 파일을 보상 삭제한다.
            deleteStoredFiles(storedFiles.values());

            throw exception;
        }

        Map<ReportFormat, Long> savedReportIds =
                reportPersistenceService.saveAll(
                        auditId,
                        ReportType.FINAL_AUDIT_REPORT,
                        storedFiles
                );

        return savedReportIds.entrySet()
                .stream()
                .map(entry ->
                        GeneratedReportResponse.completed(
                                entry.getValue(),
                                entry.getKey()
                        )
                )
                .toList();
    }

    // 생성 또는 업로드 도중 실패했을 때 이미 저장된 S3 파일을 정리한다.
    private void deleteStoredFiles(Iterable<String> s3Keys) {
        for (String s3Key : s3Keys) {
            try {
                fileStorageService.delete(s3Key);
            } catch (RuntimeException cleanupException) {
                // 정리 실패가 원래 생성 실패 예외를 덮어쓰지 않도록 로그만 남긴다.
                log.warn(
                        "보고서 생성 실패 후 S3 객체 정리에 실패했습니다. key={}",
                        s3Key,
                        cleanupException
                );
            }
        }
    }

    // 보고서 입력 데이터를 기반으로 최종 통합 보고서 본문 생성
    public String generateContent(ReportGenerationContext context) {
        String systemPrompt = ReportPromptTemplate.buildSystemPrompt();

        String userPrompt = ReportPromptBuilder.build(context);

        String generatedContent = reportLlmClient.generate(systemPrompt, userPrompt);

        ReportOutputValidator.validate(generatedContent);

        return generatedContent;
    }
}