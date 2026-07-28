package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.selfcheck.SelfCheckAnswerSaveRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.selfcheck.SelfCheckAnswerResponse;
import com.aivle13.fin_audit_ai.domain.audit.service.selfcheck.SelfCheckAnswerService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Self-Check", description = "감사 자율점검(자가진단) 응답 API")
@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
public class AuditSelfCheckController {

    private final SelfCheckAnswerService selfCheckAnswerService;

    @Operation(
            summary = "자율점검 응답 저장",
            description = "감사의 자율점검 5개 항목 응답을 저장합니다. 5개 항목을 모두, 중복 없이 제출해야 하며, "
                    + "이미 저장된 응답이 있으면 덮어씁니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "자율점검 응답 저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SelfCheckAnswerResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "감사를 찾을 수 없음")
    })
    @PostMapping("/{auditId}/self-check-answers")
    public ResponseEntity<SelfCheckAnswerResponse> save(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId,
            @Valid @RequestBody SelfCheckAnswerSaveRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        SelfCheckAnswerResponse response = selfCheckAnswerService.save(userId, auditId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "자율점검 응답 조회",
            description = "감사의 자율점검 응답을 조회합니다. 아직 제출하지 않았다면 빈 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "자율점검 응답 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SelfCheckAnswerResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "감사를 찾을 수 없음")
    })
    @GetMapping("/{auditId}/self-check-answers")
    public ResponseEntity<SelfCheckAnswerResponse> get(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        SelfCheckAnswerResponse response = selfCheckAnswerService.get(userId, auditId);
        return ResponseEntity.ok(response);
    }
}
