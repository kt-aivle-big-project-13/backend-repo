package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.ExplainabilityResponse;
import com.aivle13.fin_audit_ai.domain.audit.service.ExplainabilityService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;


@Tag(name = "Audit Explainability", description = "SHAP 설명가능성 감사 결과 API")
@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
public class AuditExplainabilityController {

    private final ExplainabilityService explainabilityService;

    @Operation(
            summary = "SHAP 설명가능성 결과 조회",
            description = "감사별 민감변수 기여비율, 전역 설명 안정성, 설명 충실성을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "SHAP 설명가능성 결과 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ExplainabilityResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "감사 또는 설명가능성 결과를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "설명가능성 분석이 아직 완료되지 않음"
            )
    })
    @GetMapping("/{auditId}/explainability")
    public ResponseEntity<ExplainabilityResponse> getExplainability(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        ExplainabilityResponse response =
                explainabilityService.getExplainability(userId, auditId);

        return ResponseEntity.ok(response);
    }
}