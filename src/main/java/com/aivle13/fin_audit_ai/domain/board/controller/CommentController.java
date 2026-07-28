package com.aivle13.fin_audit_ai.domain.board.controller;

import com.aivle13.fin_audit_ai.domain.board.dto.request.CommentCreateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.CommentUpdateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.response.CommentResponse;
import com.aivle13.fin_audit_ai.domain.board.service.CommentCommandService;
import com.aivle13.fin_audit_ai.domain.board.service.CommentQueryService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 댓글 작성/수정/삭제 경로는 SecurityConfig에서 ROLE_ADMIN으로 제한되어 있다.
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentQueryService commentQueryService;
    private final CommentCommandService commentCommandService;

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