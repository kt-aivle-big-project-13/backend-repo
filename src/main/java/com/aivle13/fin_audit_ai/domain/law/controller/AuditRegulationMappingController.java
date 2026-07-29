package com.aivle13.fin_audit_ai.domain.law.controller;

import com.aivle13.fin_audit_ai.domain.law.dto.response.RegulationMappingResponse;
import com.aivle13.fin_audit_ai.domain.law.service.AuditRegulationMappingService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Regulation Mapping", description = "RAG 기반 규제 매핑 결과 API")
@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
public class AuditRegulationMappingController {

    private final AuditRegulationMappingService auditRegulationMappingService;

    @Operation(
            summary = "RAG 기반 규제 매핑 결과 조회",
            description = "AI 기본법·금융 AI 가이드라인 조항 중 감사 결과와 자동 매핑된 법령 매핑 결과를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "규제 매핑 결과 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = RegulationMappingResponse.class
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
            )
    })
    @GetMapping("/{auditId}/regulation-mappings")
    public ResponseEntity<RegulationMappingResponse> getRegulationMappings(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        RegulationMappingResponse response = RegulationMappingResponse.of(
                auditId,
                auditRegulationMappingService.getMappings(userId, auditId)
        );

        return ResponseEntity.ok(response);
    }
}
