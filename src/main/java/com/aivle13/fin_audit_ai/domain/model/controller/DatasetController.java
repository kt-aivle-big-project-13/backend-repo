package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.dto.DatasetUploadRequestDto;
import com.aivle13.fin_audit_ai.domain.model.dto.DatasetUploadResponseDto;
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
    public ResponseEntity<DatasetUploadResponseDto> upload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long modelId,
            @ModelAttribute DatasetUploadRequestDto request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        DatasetUploadResponseDto response = datasetUploadService.upload(modelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
