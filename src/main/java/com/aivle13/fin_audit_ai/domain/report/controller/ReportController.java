package com.aivle13.fin_audit_ai.domain.report.controller;

import com.aivle13.fin_audit_ai.domain.report.dto.GeneratedReportResponse;
import com.aivle13.fin_audit_ai.domain.report.dto.ReportGenerationRequest;
import com.aivle13.fin_audit_ai.domain.report.dto.ReportGenerationResponse;
import com.aivle13.fin_audit_ai.domain.report.service.ReportDownloadResult;
import com.aivle13.fin_audit_ai.domain.report.service.ReportDownloadService;
import com.aivle13.fin_audit_ai.domain.report.service.ReportGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(
        name = "Report",
        description = "최종 감사 보고서 생성 및 다운로드 API"
)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportGenerationService reportGenerationService;
    private final ReportDownloadService reportDownloadService;

    @Operation(
            summary = "최종 감사 보고서 생성",
            description = """
                    설명 가능성 분석, 편향 진단, 규제 준수 판정,
                    개선 권고 가이드를 모두 포함한 최종 통합 보고서를 생성합니다.
                    PDF와 Word를 각각 또는 동시에 선택할 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "최종 감사 보고서 생성 성공"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "감사 정보를 찾을 수 없음"
            )
    })
    @PostMapping("/audits/{auditId}/deliverables")
    public ResponseEntity<ReportGenerationResponse> generate(
            @PathVariable Long auditId,
            @RequestBody ReportGenerationRequest request
    ) {
        List<GeneratedReportResponse> reports =
                reportGenerationService.generate(
                        auditId,
                        request.formats()
                );

        ReportGenerationResponse response =
                new ReportGenerationResponse(
                        auditId,
                        reports
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "최종 감사 보고서 다운로드",
            description = "생성된 최종 감사 보고서 PDF 또는 Word 파일을 다운로드합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "파일 다운로드 성공"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "최종 감사 보고서를 찾을 수 없음"
            )
    })
    @GetMapping("/deliverables/{reportId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long reportId
    ) {
        ReportDownloadResult result =
                reportDownloadService.download(reportId);

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(
                                result.fileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        InputStreamResource resource =
                new InputStreamResource(
                        result.file().content()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .contentType(
                        resolveMediaType(
                                result.file().contentType()
                        )
                )
                .contentLength(
                        result.file().contentLength()
                )
                .body(resource);
    }

    private MediaType resolveMediaType(
            String contentType
    ) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        return MediaType.parseMediaType(contentType);
    }
}