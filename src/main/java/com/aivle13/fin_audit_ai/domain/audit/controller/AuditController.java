package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.AuditStartRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.AuditStartResponse;
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
    public ResponseEntity<AuditStartResponse> start(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AuditStartRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        AuditStartResponse response = auditStartService.start(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
