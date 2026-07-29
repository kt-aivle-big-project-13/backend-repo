package com.aivle13.fin_audit_ai.health;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@Tag(name = "Health", description = "서버/DB 상태 확인 API")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthCheckRepository healthCheckRepository;

    @Operation(
            summary = "헬스 체크",
            description = "레코드를 하나 저장해 DB 연결 상태까지 함께 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "정상 동작",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HealthResponse.class)
                    )
            )
    })
    @GetMapping
    public HealthResponse check() {
        HealthCheck saved = healthCheckRepository.save(HealthCheck.create());
        return new HealthResponse("OK", saved.getId(), saved.getCreatedAt());
    }

    public record HealthResponse(String status, Long id, LocalDateTime checkedAt) {
    }
}
