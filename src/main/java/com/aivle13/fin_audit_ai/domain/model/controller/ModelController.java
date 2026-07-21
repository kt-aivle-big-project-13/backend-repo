package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.dto.request.ModelUploadRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.ModelUploadResponse;
import com.aivle13.fin_audit_ai.domain.model.service.ModelUploadService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelUploadService modelUploadService;

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
}
