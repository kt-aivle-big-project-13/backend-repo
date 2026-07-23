package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.FairnessResultResponse;
import com.aivle13.fin_audit_ai.domain.audit.service.FairnessResultService;
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

@Tag(name = "Audit Fairness", description = "Fairlearn 공정성 감사 결과 API")
@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
public class AuditFairnessController {

    private final FairnessResultService fairnessResultService;

    @Operation(
            summary = "공정성 분석 결과 조회",
            description = "감사별 민감정보 기준 집단 간 편향 지표(DP·EO·EOdd)를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공정성 결과 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FairnessResultResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "감사 또는 공정성 결과를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "감사가 아직 완료되지 않음")
    })
    @GetMapping("/{auditId}/fairness")
    public ResponseEntity<FairnessResultResponse> getFairness(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        FairnessResultResponse response =
                fairnessResultService.getFairness(userId, auditId);

        return ResponseEntity.ok(response);
    }
}
