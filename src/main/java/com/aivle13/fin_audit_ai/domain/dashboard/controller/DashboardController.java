package com.aivle13.fin_audit_ai.domain.dashboard.controller;

import com.aivle13.fin_audit_ai.domain.dashboard.dto.response.DashboardResponse;
import com.aivle13.fin_audit_ai.domain.dashboard.service.DashboardService;
import com.aivle13.fin_audit_ai.global.exception.user.auth.UnauthorizedException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Dashboard",
        description = "AI 모델 감사 통계 대시보드 API"
)
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // 모든 로그인 사용자에게 동일한 전체 감사 통계를 반환한다.
    @Operation(
            summary = "대시보드 조회",
            description = """
                    전체 사용자의 최신 모델 감사 결과를 집계합니다.
                    상단 요약, 감사 결과 분포, 검토 필요 모델 TOP 5,
                    공정성 지표 분포와 최근 감사 내역을 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "대시보드 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = DashboardResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
