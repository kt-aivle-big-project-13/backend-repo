package com.aivle13.fin_audit_ai.domain.board.controller;

import com.aivle13.fin_audit_ai.domain.board.dto.request.comment.CommentCreateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.comment.CommentUpdateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.response.comment.CommentResponse;
import com.aivle13.fin_audit_ai.domain.board.service.comment.CommentCommandService;
import com.aivle13.fin_audit_ai.domain.board.service.comment.CommentQueryService;
import com.aivle13.fin_audit_ai.global.exception.user.auth.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

// 댓글 작성/수정/삭제 경로는 SecurityConfig에서 ROLE_ADMIN으로 제한되어 있다.
@Tag(name = "Board Comment", description = "게시판 댓글(관리자 답변) API")
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentQueryService commentQueryService;
    private final CommentCommandService commentCommandService;

    @Operation(
            summary = "댓글 목록 조회",
            description = "게시글에 달린 댓글을 작성 순서대로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CommentResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<List<CommentResponse>> list(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(commentQueryService.list(postId));
    }

    @Operation(
            summary = "댓글 작성",
            description = "게시글에 댓글(관리자 답변)을 작성합니다. 관리자만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "댓글 작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자가 아님"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        CommentResponse response = commentCommandService.create(userId, postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "댓글 수정",
            description = "댓글 내용을 수정합니다. 관리자만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자가 아님"),
            @ApiResponse(responseCode = "404", description = "게시글 또는 댓글을 찾을 수 없음")
    })
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(commentCommandService.update(postId, commentId, request));
    }

    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 삭제합니다. 관리자만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자가 아님"),
            @ApiResponse(responseCode = "404", description = "게시글 또는 댓글을 찾을 수 없음")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        commentCommandService.delete(postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
