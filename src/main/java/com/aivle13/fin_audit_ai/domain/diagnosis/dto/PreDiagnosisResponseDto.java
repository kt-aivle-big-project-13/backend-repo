package com.aivle13.fin_audit_ai.domain.diagnosis.dto;

import com.aivle13.fin_audit_ai.domain.diagnosis.type.DiagnosisResult;

public record PreDiagnosisResponseDto(
        Long assessmentId,
        Long userId,
        Long modelId,
        boolean conditionMet,
        int groupAScore,
        int groupBScore,
        int totalScore,
        DiagnosisResult result
) {
}
