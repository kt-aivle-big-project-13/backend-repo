package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.dto.request.DatasetUploadRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.DatasetSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.dto.response.DatasetUploadResponse;
import com.aivle13.fin_audit_ai.domain.model.service.DatasetQueryService;
import com.aivle13.fin_audit_ai.domain.model.service.DatasetUploadService;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetUploadService datasetUploadService;
    private final DatasetQueryService datasetQueryService;

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

    // 같은 모델 계열(modelGroupId)에 속한 다른 버전의 데이터셋까지 포함해서 반환한다.
    // 감사/검증 데이터셋 재사용 시 프론트에서 고를 후보 목록으로 쓰인다.
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
