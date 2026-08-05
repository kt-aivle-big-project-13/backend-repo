package com.aivle13.fin_audit_ai.domain.board.controller;

import com.aivle13.fin_audit_ai.domain.board.dto.request.post.PostCreateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.post.PostPinRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.post.PostUpdateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.response.post.PostDetailResponse;
import com.aivle13.fin_audit_ai.domain.board.dto.response.post.PostSummaryResponse;
import com.aivle13.fin_audit_ai.domain.board.service.post.PostCommandService;
import com.aivle13.fin_audit_ai.domain.board.service.post.PostQueryService;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.user.auth.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Board Post", description = "게시판 게시글 API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PostQueryService postQueryService;
    private final PostCommandService postCommandService;

    @Operation(
            summary = "게시글 목록 조회",
            description = """
                    게시글 목록을 검색·정렬·페이지네이션하여 조회합니다.
                    keyword를 주면 제목/내용에 포함된 게시글만 조회하고, sort는 latest(기본)/oldest를 지원합니다.
                    공지(pinned) 게시글은 정렬 옵션과 무관하게 항상 최상단에 노출됩니다.
                    """
    )
    // 200 응답은 content를 명시하지 않아, 실제 반환 타입인 PageResponse<PostSummaryResponse>를
    // springdoc이 그대로 추론하도록 둔다(PostSummaryResponse만 명시하면 페이지네이션 래퍼가 문서에서 빠진다).
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "페이지/사이즈 값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
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

        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return ResponseEntity.ok(postQueryService.list(page, size, keyword, sort));
    }

    @Operation(
            summary = "게시글 상세 조회",
            description = "게시글 본문과 첨부파일 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostDetailResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
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

    @Operation(
            summary = "게시글 작성",
            description = "게시글을 작성합니다. 관리자뿐 아니라 로그인한 모든 사용자가 작성할 수 있으며, 첨부파일은 게시글당 최대 5개까지 등록할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "게시글 작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostDetailResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않거나 첨부파일 최대 개수를 초과함"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
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

    @Operation(
            summary = "게시글 수정",
            description = "게시글 제목/내용과 첨부파일을 수정합니다. 작성자 본인 또는 관리자만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostDetailResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않거나 첨부파일 최대 개수를 초과함"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "작성자 본인 또는 관리자가 아님"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
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

    @Operation(
            summary = "게시글 삭제",
            description = "게시글과 첨부파일·댓글을 함께 삭제합니다. 작성자 본인 또는 관리자만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "작성자 본인 또는 관리자가 아님"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
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

    @Operation(
            summary = "게시글 공지 고정/해제",
            description = "게시글을 공지로 고정하거나 해제합니다. 관리자만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공지 고정/해제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostDetailResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자가 아님"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
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

    @Operation(
            summary = "게시글 첨부파일 다운로드",
            description = "게시글에 등록된 첨부파일을 원본 파일로 다운로드합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "첨부파일 다운로드 성공",
                    content = @Content(mediaType = "application/octet-stream")
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "게시글 또는 첨부파일을 찾을 수 없음")
    })
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
