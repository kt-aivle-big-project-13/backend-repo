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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "My Page", description = "마이페이지(회원정보/비밀번호/탈퇴) API")
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class MyPageController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public MyPageController(UserService userService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @Operation(
            summary = "나의 프로필 조회",
            description = "로그인한 사용자 본인의 프로필 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    // 나의 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        UserResponse response = userService.getMyProfile(userId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "회원정보 수정",
            description = "로그인한 사용자 본인의 이름을 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원정보 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "수정 가능한 필드가 없거나 값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
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

    @Operation(
            summary = "현재 비밀번호 확인",
            description = "비밀번호 변경 전 현재 비밀번호가 맞는지 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 확인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VerifyPasswordResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자 또는 현재 비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
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

    @Operation(
            summary = "비밀번호 변경",
            description = "로그인한 사용자 본인의 비밀번호를 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChangePasswordResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "새 비밀번호가 정책에 맞지 않거나 확인값이 일치하지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자 또는 현재 비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
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

    @Operation(
            summary = "회원 탈퇴",
            description = "비밀번호 확인 후 회원을 탈퇴(소프트 삭제) 처리하고 현재 세션을 즉시 무효화합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WithdrawResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자 또는 비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
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

        // DB 탈퇴 처리는 이미 커밋되었으므로, Redis 세션 폐기가 실패해도 응답을 실패로 되돌리지 않는다.
        // 대신 로그인/재발급 시 isActive 재검사가 잔여 세션을 막아주는 보완 통제 역할을 한다.
        try {
            refreshTokenService.revoke(userId, accessToken);
        } catch (Exception e) {
            log.error("탈퇴 처리 후 리프레시 세션/액세스 토큰 폐기 실패. userId={}", userId, e);
        }

        return ResponseEntity.ok(response);
    }
}
