package com.aivle13.fin_audit_ai.domain.report.controller;

import com.aivle13.fin_audit_ai.domain.report.dto.response.highimpact.HighImpactReportGenerationResponse;
import com.aivle13.fin_audit_ai.domain.report.dto.response.highimpact.HighImpactReportMetadataResponse;
import com.aivle13.fin_audit_ai.domain.report.service.highimpact.HighImpactReportGenerationService;
import com.aivle13.fin_audit_ai.domain.report.service.highimpact.HighImpactReportQueryService;
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
        name = "High Impact Report",
        description = "고영향 AI 사전진단 보고서 API"
)
@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
public class HighImpactReportController {

    private final HighImpactReportGenerationService generationService;
    private final HighImpactReportQueryService queryService;

    @Operation(
            summary = "고영향 AI 사전진단 보고서 생성",
            description = "감사에 연결된 사전진단 결과를 기반으로 "
                    + "PDF·Word 보고서를 생성하고 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "보고서 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            HighImpactReportGenerationResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "고영향으로 확정되지 않은 사전진단"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "감사 또는 연결된 사전진단을 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "저장된 사전진단 데이터 또는 AI 응답이 올바르지 않음"
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "AI 서버 보고서 생성 실패"
            ),
            @ApiResponse(
                    responseCode = "504",
                    description = "AI 서버 응답 시간 초과"
            )
    })
    @PostMapping(
            "/{auditId}/reports/high-impact-assessment"
    )
    public ResponseEntity<HighImpactReportGenerationResponse> generate(
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
                        new HighImpactReportGenerationResponse(
                                reportId
                        )
                );
    }

    @Operation(
            summary = "최신 고영향 AI 사전진단 보고서 조회",
            description = "PDF 또는 Word 형식의 최신 고영향 AI "
                    + "사전진단 보고서 메타데이터를 조회합니다."
    )
    @GetMapping(
            "/{auditId}/reports/high-impact-assessment"
    )
    public ResponseEntity<HighImpactReportMetadataResponse> getLatest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId,
            @RequestParam(defaultValue = "PDF")
            ReportFormat format
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(
                queryService.getLatest(
                        userId,
                        auditId,
                        format
                )
        );
    }

    @Operation(
            summary = "고영향 AI 사전진단 보고서 다운로드",
            description = "생성된 PDF 또는 Word 형식의 고영향 AI "
                    + "사전진단 보고서를 다운로드합니다."
    )
    @GetMapping(
            "/{auditId}/reports/high-impact-assessment/"
                    + "{reportId}/download"
    )
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId,
            @PathVariable Long reportId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        HighImpactReportQueryService.ReportDownload download =
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
                        : MediaType.APPLICATION_OCTET_STREAM;

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
