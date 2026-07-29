package com.aivle13.fin_audit_ai.domain.notification.controller;

import com.aivle13.fin_audit_ai.domain.notification.dto.response.NotificationResponse;
import com.aivle13.fin_audit_ai.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.aivle13.fin_audit_ai.domain.notification.service.NotificationService;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.user.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    // 알림 벨 목록 (최신순 페이징)
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return ResponseEntity.ok(notificationService.list(userId, page, size));
    }

    // 알림 벨 배지에 표시할 안읽음 개수
    @GetMapping("/unread-count")
    public ResponseEntity<NotificationUnreadCountResponse> unreadCount(
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        return ResponseEntity.ok(new NotificationUnreadCountResponse(notificationService.countUnread(userId)));
    }

    // 알림 단건 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    // 알림 전체 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new UnauthorizedException();
        }

        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}