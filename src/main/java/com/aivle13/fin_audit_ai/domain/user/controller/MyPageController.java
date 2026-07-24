package com.aivle13.fin_audit_ai.domain.user.controller;

import com.aivle13.fin_audit_ai.domain.user.dto.request.ChangePasswordRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.UpdateNameRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.response.ChangePasswordResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.UserResponse;
import com.aivle13.fin_audit_ai.domain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class MyPageController {

    private final UserService userService;

    public MyPageController(UserService userService) {
        this.userService = userService;
    }

    // 나의 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        UserResponse response = userService.getMyProfile(userId);

        return ResponseEntity.ok(response);
    }

    // 회원정보 수정
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateNameRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        UserResponse response = userService.updateMyProfile(userId, request);

        return ResponseEntity.ok(response);
    }

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public ResponseEntity<ChangePasswordResponse> changeMyPassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        ChangePasswordResponse response = userService.changeMyPassword(userId, request);

        return ResponseEntity.ok(response);
    }
}