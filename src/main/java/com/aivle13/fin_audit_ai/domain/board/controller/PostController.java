package com.aivle13.fin_audit_ai.domain.board.controller;

import com.aivle13.fin_audit_ai.domain.board.dto.request.PostCreateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.PostPinRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.PostUpdateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.response.PostDetailResponse;
import com.aivle13.fin_audit_ai.domain.board.dto.response.PostSummaryResponse;
import com.aivle13.fin_audit_ai.domain.board.service.PostCommandService;
import com.aivle13.fin_audit_ai.domain.board.service.PostQueryService;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostQueryService postQueryService;
    private final PostCommandService postCommandService;

    // 게시글 목록 (검색 + 정렬 + 페이지네이션, 공지는 항상 최상단)
    @GetMapping
    public ResponseEntity<PageResponse<PostSummaryResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(postQueryService.list(page, size, keyword, sort));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> get(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(postQueryService.getDetail(postId));
    }

    // 게시글 작성. 관리자뿐 아니라 로그인한 모든 사용자가 작성할 수 있다.
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PostDetailResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute PostCreateRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        PostDetailResponse response = postCommandService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 게시글 수정. 작성자 본인 또는 관리자만 가능(서비스 계층에서 검증).
    @PatchMapping(path = "/{postId}", consumes = "multipart/form-data")
    public ResponseEntity<PostDetailResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @ModelAttribute PostUpdateRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(postCommandService.update(userId, postId, request));
    }

    // 게시글 삭제. 작성자 본인 또는 관리자만 가능(서비스 계층에서 검증).
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        postCommandService.delete(userId, postId);
        return ResponseEntity.noContent().build();
    }

    // 공지 고정/해제. 관리자만 가능(SecurityConfig에서 경로 자체를 ROLE_ADMIN으로 제한).
    @PatchMapping("/{postId}/pin")
    public ResponseEntity<PostDetailResponse> pin(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody PostPinRequest request
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(postCommandService.updatePinned(userId, postId, request.pinned()));
    }

    @GetMapping("/{postId}/attachments/{attachmentId}")
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @PathVariable Long attachmentId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        PostQueryService.AttachmentDownload download = postQueryService.downloadAttachment(postId, attachmentId);

        String encodedName = URLEncoder.encode(download.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType contentType = download.contentType() != null
                ? MediaType.parseMediaType(download.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(download.size())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"; filename*=UTF-8''" + encodedName
                )
                .body(new InputStreamResource(download.content()));
    }
}