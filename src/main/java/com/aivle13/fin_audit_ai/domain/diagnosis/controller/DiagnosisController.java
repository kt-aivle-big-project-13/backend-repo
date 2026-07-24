package com.aivle13.fin_audit_ai.domain.diagnosis.controller;

import com.aivle13.fin_audit_ai.domain.diagnosis.dto.PreDiagnosisRequestDto;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.PreDiagnosisQuantitativeRequestDto;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.PreDiagnosisResponseDto;
import com.aivle13.fin_audit_ai.domain.diagnosis.service.DiagnosisService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping("/impact-assessments")
    public ResponseEntity<PreDiagnosisResponseDto> start(
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(diagnosisService.start(userId));
    }

    @PostMapping("/impact-assessments/{assessmentId}/stage1")
    public ResponseEntity<PreDiagnosisResponseDto> diagnoseQualitative(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody PreDiagnosisRequestDto request
    ) {
        return ResponseEntity.ok(diagnosisService.diagnoseQualitative(assessmentId, request));
    }

    @PostMapping("/impact-assessments/{assessmentId}/stage2")
    public ResponseEntity<PreDiagnosisResponseDto> diagnoseQuantitative(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody PreDiagnosisQuantitativeRequestDto request
    ) {
        return ResponseEntity.ok(diagnosisService.diagnoseQuantitative(assessmentId, request));
    }

    @GetMapping("/impact-assessments/{assessmentId}")
    public ResponseEntity<PreDiagnosisResponseDto> getResult(
            @PathVariable("assessmentId") Long assessmentId
    ) {
        return ResponseEntity.ok(diagnosisService.getResult(assessmentId));
    }
}
