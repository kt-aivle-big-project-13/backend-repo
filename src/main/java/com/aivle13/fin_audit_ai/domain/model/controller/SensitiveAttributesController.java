package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.dto.request.sensitiveattributes.SensitiveAttributesRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.sensitiveattributes.SensitiveAttributesResponse;
import com.aivle13.fin_audit_ai.domain.model.service.sensitiveattributes.SensitiveAttributesService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Sensitive Attributes", description = "데이터셋 민감정보(보호속성) 설정 API")
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class SensitiveAttributesController {

    private final SensitiveAttributesService sensitiveAttributesService;

    @Operation(
            summary = "데이터셋 민감정보(보호속성) 설정",
            description = "공정성 감사에 사용할 민감정보(보호속성) 컬럼을 지정합니다. "
                    + "AUDIT 용도의 데이터셋에만 설정할 수 있고, 이미 감사에 사용된 데이터셋은 수정할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "민감정보 설정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SensitiveAttributesResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "데이터셋에 존재하지 않는 컬럼을 지정함"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "데이터셋을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 감사에 사용된 데이터셋은 민감정보를 수정할 수 없음")
    })
    @PatchMapping("/{modelId}/datasets/{datasetId}/sensitive-attributes")
    public ResponseEntity<SensitiveAttributesResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long modelId,
            @PathVariable Long datasetId,
            @Valid @RequestBody SensitiveAttributesRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        SensitiveAttributesResponse response = sensitiveAttributesService.update(userId, modelId, datasetId, request);
        return ResponseEntity.ok(response);
    }
}
