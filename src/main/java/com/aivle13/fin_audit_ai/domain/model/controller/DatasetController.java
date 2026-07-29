package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.dto.request.dataset.DatasetUploadRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.dataset.DatasetSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.dto.response.dataset.DatasetUploadResponse;
import com.aivle13.fin_audit_ai.domain.model.service.dataset.DatasetQueryService;
import com.aivle13.fin_audit_ai.domain.model.service.dataset.DatasetUploadService;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dataset", description = "모델 데이터셋 업로드/조회 API")
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetUploadService datasetUploadService;
    private final DatasetQueryService datasetQueryService;

    @Operation(
            summary = "데이터셋 업로드",
            description = "모델에 사용할 CSV 데이터셋을 업로드합니다. purpose를 주지 않으면 AUDIT(감사용)으로 등록됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "데이터셋 업로드 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatasetUploadResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "CSV 파일을 읽을 수 없거나 지원하지 않는 형식"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "모델을 찾을 수 없음"),
            @ApiResponse(responseCode = "413", description = "파일 크기가 허용 범위를 초과함")
    })
    @PostMapping(path = "/{modelId}/datasets", consumes = "multipart/form-data")
    public ResponseEntity<DatasetUploadResponse> upload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long modelId,
            @ModelAttribute DatasetUploadRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        DatasetUploadResponse response = datasetUploadService.upload(userId, modelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "데이터셋 목록 조회",
            description = "모델이 속한 모델 계열(같은 modelGroupId)의 데이터셋 목록을 조회합니다. "
                    + "purpose를 주면 AUDIT/VALIDATION 용도로 필터링합니다. "
                    + "감사/검증 데이터셋 재사용 시 프론트에서 고를 후보 목록으로 쓰입니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "데이터셋 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DatasetSummaryResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "모델을 찾을 수 없음")
    })
    @GetMapping("/{modelId}/datasets")
    public ResponseEntity<List<DatasetSummaryResponse>> list(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long modelId,
            @RequestParam(required = false) DatasetPurpose purpose
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        List<DatasetSummaryResponse> response = datasetQueryService.list(userId, modelId, purpose);
        return ResponseEntity.ok(response);
    }
}
