package com.aivle13.fin_audit_ai.domain.report.controller;

import com.aivle13.fin_audit_ai.domain.report.dto.response.explainability.ExplainabilityReportGenerationResponse;
import com.aivle13.fin_audit_ai.domain.report.dto.response.explainability.ExplainabilityReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.service.explainability.ExplainabilityReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.explainability.ExplainabilityReportQueryService;
import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.global.exception.user.auth.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(
        name = "Explainability Report",
        description = "설명가능성 HTML 리포트 API"
)
@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
public class ExplainabilityReportController {

    private final ExplainabilityReportGenerationService generationService;
    private final ExplainabilityReportQueryService queryService;

    @Operation(
            summary = "설명가능성 HTML 리포트 생성",
            description = "SHAP 분석 결과를 기반으로 설명가능성 HTML 리포트를 생성하고 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "설명가능성 리포트 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            ExplainabilityReportGenerationResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "감사를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "AI 서버 리포트 생성 실패"
            ),
            @ApiResponse(
                    responseCode = "504",
                    description = "AI 서버 응답 시간 초과"
            )
    })
    @PostMapping("/{auditId}/reports/explainability")
    public ResponseEntity<ExplainabilityReportGenerationResponse> generate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        Long reportId = generationService.generateAndSave(
                userId,
                auditId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new ExplainabilityReportGenerationResponse(
                                reportId
                        )
                );
    }

    @Operation(
            summary = "최신 설명가능성 리포트 조회",
            description = "감사에서 가장 최근에 생성된 설명가능성 리포트 메타데이터를 조회합니다."
    )
    @GetMapping("/{auditId}/reports/explainability")
    public ResponseEntity<ExplainabilityReportMetadataResponse> getLatest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId,
            @RequestParam(defaultValue = "HTML")
            ReportFormat format
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(
                queryService.getLatest(userId, auditId, format)
        );
    }

    @Operation(
            summary = "설명가능성 HTML 리포트 다운로드",
            description = "생성된 설명가능성 HTML 리포트를 다운로드합니다."
    )
    @GetMapping(
            "/{auditId}/reports/explainability/{reportId}/download"
    )
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId,
            @PathVariable Long reportId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        ExplainabilityReportQueryService.ReportDownload download =
                queryService.download(
                        userId,
                        auditId,
                        reportId
                );

        String encodedName = URLEncoder.encode(
                download.filename(),
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        MediaType contentType =
                download.contentType() != null
                        ? MediaType.parseMediaType(
                                download.contentType()
                        )
                        : MediaType.TEXT_HTML;

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(download.size())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + download.filename()
                                + "\"; filename*=UTF-8''"
                                + encodedName
                )
                .body(
                        new InputStreamResource(
                                download.content()
                        )
                );
    }
}
