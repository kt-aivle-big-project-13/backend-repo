package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditStartRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditStartResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditSummaryResponse;
import com.aivle13.fin_audit_ai.domain.audit.service.AuditService;
import com.aivle13.fin_audit_ai.domain.audit.service.AuditStartService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Audit",
        description = "AI 모델 감사 실행 API"
)
@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
public class AuditController {

    private final AuditStartService auditStartService;
    private final AuditService auditService;

    @Operation(
            summary = "AI 모델 감사 시작",
            description = """
                    등록된 모델과 데이터셋으로 감사를 생성합니다.
                    요청이 정상 처리되면 202 Accepted와 PENDING 상태를 반환합니다.
                    감사 생성 트랜잭션이 커밋된 후 SHAP 설명가능성 분석이 비동기로 실행됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "감사 생성 및 비동기 분석 요청 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = AuditStartResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값이 올바르지 않음"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "모델 또는 데이터셋을 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "감사를 시작할 수 없는 상태"
            )
    })
    @PostMapping
    public ResponseEntity<AuditStartResponse> start(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AuditStartRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        AuditStartResponse response = auditStartService.start(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(
            summary = "감사 이력 목록 조회",
            description = "사용자가 실행한 감사 목록을 최신순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "감사 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuditSummaryResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping
    public ResponseEntity<List<AuditSummaryResponse>> list(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(auditService.list(userId));
    }
}
