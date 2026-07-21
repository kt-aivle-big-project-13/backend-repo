package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.dto.request.DatasetUploadRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.DatasetUploadResponse;
import com.aivle13.fin_audit_ai.domain.model.service.DatasetUploadService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetUploadService datasetUploadService;

    @PostMapping(path = "/{modelId}/datasets", consumes = "multipart/form-data")
    public ResponseEntity<DatasetUploadResponse> upload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long modelId,
            @ModelAttribute DatasetUploadRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        DatasetUploadResponse response = datasetUploadService.upload(modelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
