package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.core.AuditStartRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.core.AuditRetryResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.core.AuditStartResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.core.AuditSummaryResponse;
import com.aivle13.fin_audit_ai.domain.audit.service.core.AuditService;
import com.aivle13.fin_audit_ai.domain.audit.service.core.AuditStartService;
import com.aivle13.fin_audit_ai.global.exception.user.auth.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
                            array = @ArraySchema(schema = @Schema(implementation = AuditSummaryResponse.class))
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

    @Operation(
            summary = "감사 취소",
            description = """
                    대기 중이거나 진행 중인 감사를 취소합니다.
                    AI 서버로 이미 나간 분석 요청 자체를 끊지는 않지만(soft cancel),
                    그 결과는 무시되고 감사는 CANCELLED 상태로 유지됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "감사 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "감사를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "취소할 수 없는 감사 상태")
    })
    @PostMapping("/{auditId}/cancel")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        auditService.cancel(auditId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "감사 재시도",
            description = """
                    FAILED 또는 CANCELLED 상태인 감사를 기존 모델·데이터셋 참조 그대로 다시 실행합니다.
                    기존 SHAP·공정성 분석 결과는 초기화되고, 감사는 PENDING 상태로 돌아가
                    SHAP 설명가능성 분석이 비동기로 다시 실행됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "감사 재시도 요청 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = AuditRetryResponse.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "감사를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "재시도할 수 없는 감사 상태 (FAILED/CANCELLED 아님)")
    })
    @PostMapping("/{auditId}/retry")
    public ResponseEntity<AuditRetryResponse> retry(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        AuditRetryResponse response = auditService.retry(auditId, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
