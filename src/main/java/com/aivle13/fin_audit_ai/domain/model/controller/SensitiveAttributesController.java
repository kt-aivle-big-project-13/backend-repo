package com.aivle13.fin_audit_ai.domain.model.controller;

import com.aivle13.fin_audit_ai.domain.model.dto.request.SensitiveAttributesRequest;
import com.aivle13.fin_audit_ai.domain.model.dto.response.SensitiveAttributesResponse;
import com.aivle13.fin_audit_ai.domain.model.service.SensitiveAttributesService;
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

    @PatchMapping("/{modelId}/sensitive-attributes")
    public ResponseEntity<SensitiveAttributesResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long modelId,
            @Valid @RequestBody SensitiveAttributesRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        SensitiveAttributesResponse response = sensitiveAttributesService.update(modelId, request);
        return ResponseEntity.ok(response);
    }
}
