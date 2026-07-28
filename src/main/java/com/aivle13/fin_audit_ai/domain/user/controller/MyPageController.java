package com.aivle13.fin_audit_ai.domain.user.controller;

import com.aivle13.fin_audit_ai.domain.auth.service.RefreshTokenService;
import com.aivle13.fin_audit_ai.domain.user.dto.request.password.ChangePasswordRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.password.VerifyPasswordRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.profile.UpdateNameRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.withdraw.WithdrawRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.ChangePasswordResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.VerifyPasswordResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.profile.UserResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.withdraw.WithdrawResponse;
import com.aivle13.fin_audit_ai.domain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class MyPageController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public MyPageController(UserService userService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
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

    // 비밀번호 변경 전 현재 비밀번호 확인
    @PostMapping("/me/password/verify")
    public ResponseEntity<VerifyPasswordResponse> verifyCurrentPassword(
            Authentication authentication,
            @Valid @RequestBody VerifyPasswordRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        VerifyPasswordResponse response = userService.verifyCurrentPassword(userId, request);

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

    // 회원 탈퇴. 성공 시 현재 세션도 즉시 무효화한다(로그아웃과 동일하게
    // 리프레시 세션 삭제 + 현재 액세스 토큰 블랙리스트 등록).
    @DeleteMapping("/me")
    public ResponseEntity<WithdrawResponse> withdraw(
            Authentication authentication,
            @Valid @RequestBody WithdrawRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String accessToken = (String) authentication.getCredentials();

        WithdrawResponse response = userService.withdraw(userId, request);

        refreshTokenService.revoke(userId, accessToken);

        return ResponseEntity.ok(response);
    }
}