package com.aivle13.fin_audit_ai.domain.audit.controller;

import com.aivle13.fin_audit_ai.domain.audit.dto.AuditUploadRequestDto;
import com.aivle13.fin_audit_ai.domain.audit.dto.AuditUploadResponseDto;
import com.aivle13.fin_audit_ai.domain.audit.service.AuditUploadService;
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
public class AuditUploadController {

    private final AuditUploadService auditUploadService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<AuditUploadResponseDto> upload(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute AuditUploadRequestDto request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        AuditUploadResponseDto response = auditUploadService.upload(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
