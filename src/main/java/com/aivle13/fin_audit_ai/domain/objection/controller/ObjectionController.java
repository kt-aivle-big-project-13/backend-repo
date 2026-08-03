package com.aivle13.fin_audit_ai.domain.objection.controller;

import com.aivle13.fin_audit_ai.domain.objection.dto.request.ObjectionDispatchRequest;
import com.aivle13.fin_audit_ai.domain.objection.dto.request.ObjectionImportRequest;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.LetterBodyResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionDetailResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionDocumentResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionImportResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionSummaryResponse;
import com.aivle13.fin_audit_ai.domain.objection.service.ObjectionCommandService;
import com.aivle13.fin_audit_ai.domain.objection.service.ObjectionQueryService;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Objection", description = "고객 이의제기 API")
@RestController
@RequestMapping("/api/v1/objections")
@RequiredArgsConstructor
public class ObjectionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ObjectionQueryService objectionQueryService;
    private final ObjectionCommandService objectionCommandService;

    @Operation(
            summary = "이의제기 CSV 업로드",
            description = "대상 모델 ID와 고객 이의제기 CSV를 업로드해 이의제기 건을 일괄 등록합니다. "
                    + "CSV 필수 컬럼은 고객_이름, 이의제기_번호, 거절_금융기준, 제목, 내용, "
                    + "주요_판단_근거_변수, 담당자_판단_근거, 작성일시입니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "일괄 등록 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ObjectionImportResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "CSV 형식 또는 내용이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 이의제기 번호가 포함됨")
    })
    @PostMapping(path = "/import", consumes = "multipart/form-data")
    public ResponseEntity<ObjectionImportResponse> importCsv(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute ObjectionImportRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(objectionCommandService.importFromCsv(userId, request.modelId(), request.file()));
    }

    @Operation(
            summary = "이의제기 목록 조회",
            description = "이의제기 목록을 검색·상태별·정렬·페이지네이션하여 조회합니다. "
                    + "status는 WAITING(답변대기)/COMPLETED(답변완료), sort는 latest(기본)/oldest를 지원합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이의제기 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "페이지/사이즈/상태 값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping
    public ResponseEntity<PageResponse<ObjectionSummaryResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return ResponseEntity.ok(objectionQueryService.list(userId, page, size, status, keyword, sort));
    }

    @Operation(summary = "이의제기 상세 조회", description = "이의제기 상세 정보와 판단 근거, 처리 상태를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이의제기 상세 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ObjectionDetailResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "이의제기를 찾을 수 없음")
    })
    @GetMapping("/{objectionId}")
    public ResponseEntity<ObjectionDetailResponse> get(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long objectionId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(objectionQueryService.getDetail(userId, objectionId));
    }

    @Operation(
            summary = "대응문서 조회",
            description = "처리 결과(decision)에 따른 판단 근거 설명과 고객 안내문 초안을 조회합니다. "
                    + "이미 발송된 건은 발송 당시 확정된 내용을 그대로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대응문서 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "이의제기를 찾을 수 없음")
    })
    @GetMapping("/{objectionId}/document")
    public ResponseEntity<ObjectionDocumentResponse> getDocument(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long objectionId,
            @RequestParam(required = false) ObjectionDecision decision
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(objectionQueryService.getDocument(userId, objectionId, decision));
    }

    @Operation(summary = "고객 안내문 재생성", description = "처리 결과에 맞는 고객 안내문을 다시 생성합니다. 발송 완료된 건은 재생성할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "이의제기를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 발송 완료된 이의제기")
    })
    @PostMapping("/{objectionId}/document/regenerate")
    public ResponseEntity<LetterBodyResponse> regenerate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long objectionId,
            @RequestParam(required = false) ObjectionDecision decision
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(objectionQueryService.regenerateLetter(userId, objectionId, decision));
    }

    @Operation(
            summary = "이의제기 처리 확정 및 고객 안내문 발송",
            description = "처리 결과(거절 유지/재심사)를 확정하고 입력한 이메일로 고객 안내문을 발송 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음(이메일 형식 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "이의제기를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 발송 완료된 이의제기")
    })
    @PostMapping("/{objectionId}/dispatch")
    public ResponseEntity<ObjectionDetailResponse> dispatch(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long objectionId,
            @Valid @RequestBody ObjectionDispatchRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(objectionCommandService.dispatch(userId, objectionId, request));
    }
}