package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.dto.request.sensitiveattributes.SensitiveAttributesRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.sensitiveattributes.SensitiveAttributesResponse;
import com.aivle13.fin_audit_ai.domain.model.service.sensitiveattributes.SensitiveAttributesService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class SensitiveAttributesController {

    private final SensitiveAttributesService sensitiveAttributesService;

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
