package com.aivle13.fin_audit_ai.domain.diagnosis.controller;

import com.aivle13.fin_audit_ai.domain.diagnosis.dto.request.PreDiagnosisRequest;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.request.PreDiagnosisQuantitativeRequest;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.response.PreDiagnosisResponse;
import com.aivle13.fin_audit_ai.domain.diagnosis.service.DiagnosisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/models/{modelId}/impact-assessments")
    public ResponseEntity<PreDiagnosisResponse> start(
            @PathVariable Long modelId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(diagnosisService.start(modelId));
    }

    @PostMapping("/impact-assessments/{assessmentId}/stage1")
    public ResponseEntity<PreDiagnosisResponse> diagnoseQualitative(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody PreDiagnosisRequest request
    ) {
        return ResponseEntity.ok(diagnosisService.diagnoseQualitative(assessmentId, request));
    }

    @PostMapping("/impact-assessments/{assessmentId}/stage2")
    public ResponseEntity<PreDiagnosisResponse> diagnoseQuantitative(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody PreDiagnosisQuantitativeRequest request
    ) {
        return ResponseEntity.ok(diagnosisService.diagnoseQuantitative(assessmentId, request));
    }

    @GetMapping("/impact-assessments/{assessmentId}")
    public ResponseEntity<PreDiagnosisResponse> getResult(
            @PathVariable("assessmentId") Long assessmentId
    ) {
        return ResponseEntity.ok(diagnosisService.getResult(assessmentId));
    }
}
