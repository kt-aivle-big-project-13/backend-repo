package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.dto.request.model.ModelUploadRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.model.ModelSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.dto.response.model.ModelUploadResponse;
import com.aivle13.fin_audit_ai.domain.model.service.model.ModelQueryService;
import com.aivle13.fin_audit_ai.domain.model.service.model.ModelUploadService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
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

@Tag(name = "Model", description = "AI 모델 등록/조회 API")
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelUploadService modelUploadService;
    private final ModelQueryService modelQueryService;

    @Operation(
            summary = "AI 모델 등록",
            description = "감사 대상 AI 모델 파일을 업로드하여 등록합니다. previousModelId를 주면 기존 모델의 새 버전으로 등록됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "모델 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ModelUploadResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않거나 지원하지 않는 모델 파일 형식"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "이전 버전 모델(previousModelId)을 찾을 수 없음"),
            @ApiResponse(responseCode = "413", description = "파일 크기가 허용 범위를 초과함")
    })
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ModelUploadResponse> upload(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute ModelUploadRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        ModelUploadResponse response = modelUploadService.upload(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "AI 모델 목록 조회",
            description = "내가 등록한 AI 모델 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "모델 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ModelSummaryResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping
    public ResponseEntity<List<ModelSummaryResponse>> list(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(modelQueryService.list(userId));
    }
}
