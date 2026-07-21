package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditStartRequestDto;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditStartResponseDto;
import com.aivle13.fin_audit_ai.domain.audit.service.AuditStartService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final AuditStartService auditStartService;

    @PostMapping
    public ResponseEntity<AuditStartResponseDto> start(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AuditStartRequestDto request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        AuditStartResponseDto response = auditStartService.start(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
