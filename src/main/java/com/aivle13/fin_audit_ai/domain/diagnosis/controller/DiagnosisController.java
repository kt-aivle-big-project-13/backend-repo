package com.aivle13.fin_audit_ai.domain.diagnosis.controller;

import com.aivle13.fin_audit_ai.domain.diagnosis.dto.request.PreDiagnosisRequest;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.request.PreDiagnosisQuantitativeRequest;
import com.aivle13.fin_audit_ai.domain.diagnosis.dto.response.PreDiagnosisResponse;
import com.aivle13.fin_audit_ai.domain.diagnosis.service.DiagnosisService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Diagnosis", description = "사전진단(영향평가) API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @Operation(
            summary = "사전진단 시작",
            description = "새 사전진단(영향평가)을 생성하고 IN_PROGRESS 상태로 시작합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "사전진단 시작 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PreDiagnosisResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/impact-assessments")
    public ResponseEntity<PreDiagnosisResponse> start(
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(diagnosisService.start(userId));
    }

    @Operation(
            summary = "정성 진단(1단계, GATE) 응답 제출",
            description = "GATE_01, GATE_02 두 문항에 모두 응답을 제출합니다. 하나라도 해당되면 HIGH_IMPACT로, "
                    + "아니면 정량 진단(2단계)이 필요한 상태로 전이합니다. IN_PROGRESS 상태의 사전진단에만 제출할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "정성 진단 응답 저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PreDiagnosisResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 문항이 올바르지 않거나 진단 상태가 맞지 않음"),
            @ApiResponse(responseCode = "404", description = "사전진단을 찾을 수 없음")
    })
    @PostMapping("/impact-assessments/{assessmentId}/stage1")
    public ResponseEntity<PreDiagnosisResponse> diagnoseQualitative(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody PreDiagnosisRequest request
    ) {
        return ResponseEntity.ok(diagnosisService.diagnoseQualitative(assessmentId, request));
    }

    @Operation(
            summary = "정량 진단(2단계) 응답 제출",
            description = "A_01~A_03, B_01~B_03 문항에 모두 응답을 제출합니다. 가중 점수 합이 임계값 이상이면 HIGH_IMPACT, "
                    + "아니면 NOT_APPLICABLE로 결과가 확정됩니다. 1단계(GATE)를 통과한 사전진단에만 제출할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "정량 진단 응답 저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PreDiagnosisResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 문항이 올바르지 않거나 진단 상태가 맞지 않음"),
            @ApiResponse(responseCode = "404", description = "사전진단을 찾을 수 없음")
    })
    @PostMapping("/impact-assessments/{assessmentId}/stage2")
    public ResponseEntity<PreDiagnosisResponse> diagnoseQuantitative(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody PreDiagnosisQuantitativeRequest request
    ) {
        return ResponseEntity.ok(diagnosisService.diagnoseQuantitative(assessmentId, request));
    }

    @Operation(
            summary = "사전진단 결과 조회",
            description = "사전진단의 현재 진행 상태와 진단 결과를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사전진단 결과 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PreDiagnosisResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "사전진단을 찾을 수 없음")
    })
    @GetMapping("/impact-assessments/{assessmentId}")
    public ResponseEntity<PreDiagnosisResponse> getResult(
            @PathVariable("assessmentId") Long assessmentId
    ) {
        return ResponseEntity.ok(diagnosisService.getResult(assessmentId));
    }
}
