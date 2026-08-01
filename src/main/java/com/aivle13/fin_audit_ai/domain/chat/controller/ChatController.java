package com.aivle13.fin_audit_ai.domain.chat.controller;

import com.aivle13.fin_audit_ai.domain.chat.dto.request.ChatConversationCreateRequest;
import com.aivle13.fin_audit_ai.domain.chat.dto.request.ChatMessageCreateRequest;
import com.aivle13.fin_audit_ai.domain.chat.dto.response.ChatConversationResponse;
import com.aivle13.fin_audit_ai.domain.chat.dto.response.ChatMessageResponse;
import com.aivle13.fin_audit_ai.domain.chat.service.ChatConversationService;
import com.aivle13.fin_audit_ai.domain.chat.service.ChatMessageService;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "Chat",
        description = "감사 결과 질의 챗봇 API"
)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatConversationService conversationService;
    private final ChatMessageService messageService;

    @Operation(
            summary = "질의 대화 시작",
            description = "감사 결과에 대해 질문할 대화를 만듭니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "대화 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "감사를 찾을 수 없음")
    })
    @PostMapping("/audits/{auditId}/conversations")
    public ResponseEntity<ChatConversationResponse> createConversation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId,
            @Valid @RequestBody(required = false)
            ChatConversationCreateRequest request
    ) {
        requireLogin(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(conversationService.create(
                        userId,
                        auditId,
                        request == null ? null : request.title()
                ));
    }

    @Operation(
            summary = "질의 대화 목록",
            description = "해당 감사에서 내가 만든 대화를 최신순으로 조회합니다."
    )
    @GetMapping("/audits/{auditId}/conversations")
    public ResponseEntity<List<ChatConversationResponse>> getConversations(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long auditId
    ) {
        requireLogin(userId);

        return ResponseEntity.ok(
                conversationService.getAll(userId, auditId)
        );
    }

    @Operation(
            summary = "질문",
            description = """
                    감사 결과를 근거로 질문에 답합니다. 답변에는 인용과 근거 충실도
                    (GROUNDED·PARTIAL·NOT_GROUNDED)가 함께 반환됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "답변 생성 성공"),
            @ApiResponse(responseCode = "400", description = "질문이 비었거나 너무 김"),
            @ApiResponse(responseCode = "404", description = "대화를 찾을 수 없음"),
            @ApiResponse(responseCode = "429", description = "일일 질문 수 초과"),
            @ApiResponse(responseCode = "502", description = "AI 서버 요청 실패"),
            @ApiResponse(responseCode = "504", description = "AI 서버 응답 시간 초과")
    })
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ChatMessageResponse> ask(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long conversationId,
            @Valid @RequestBody ChatMessageCreateRequest request
    ) {
        requireLogin(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(messageService.ask(
                        userId,
                        conversationId,
                        request.question()
                ));
    }

    @Operation(
            summary = "대화 이력 조회",
            description = "대화의 질문·답변을 시간순으로 조회합니다."
    )
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long conversationId
    ) {
        requireLogin(userId);

        return ResponseEntity.ok(
                messageService.getMessages(userId, conversationId)
        );
    }

    @Operation(
            summary = "대화 삭제",
            description = "대화와 그 안의 질문·답변·인용을 함께 삭제합니다."
    )
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long conversationId
    ) {
        requireLogin(userId);

        conversationService.delete(userId, conversationId);

        return ResponseEntity.noContent().build();
    }

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException();
        }
    }
}
