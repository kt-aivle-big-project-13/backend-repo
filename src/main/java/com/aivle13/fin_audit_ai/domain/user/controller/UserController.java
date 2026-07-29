package com.aivle13.fin_audit_ai.domain.user.controller;

import com.aivle13.fin_audit_ai.domain.user.dto.request.email.EmailVerificationConfirmRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.email.EmailVerificationRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.password.PasswordFindRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.password.PasswordResetRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.response.email.EmailVerificationConfirmResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.email.EmailVerificationResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.PasswordFindResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.PasswordResetResponse;
import com.aivle13.fin_audit_ai.domain.user.service.email.EmailVerificationService;
import com.aivle13.fin_audit_ai.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth Email/Password", description = "이메일 인증, 비밀번호 찾기/재설정 API")
@RestController
@RequestMapping("/api/v1/auth")
public class UserController {

    private final UserService userService;
    private final EmailVerificationService
            emailVerificationService;

    public UserController(
            UserService userService,
            EmailVerificationService emailVerificationService
    ) {
        this.userService = userService;
        this.emailVerificationService =
                emailVerificationService;
    }

    @Operation(
            summary = "이메일 인증번호 발송",
            description = "회원가입에 사용할 이메일로 인증번호를 발송합니다. 이미 가입된 이메일이면 발송하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인증번호 발송 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EmailVerificationResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    // 이메일 인증번호 발송
    @PostMapping("/email/verification-code")
    public ResponseEntity<EmailVerificationResponse>
    sendVerificationCode(
            @Valid
            @RequestBody
            EmailVerificationRequest request
    ) {
        EmailVerificationResponse response =
                emailVerificationService
                        .sendVerificationCode(
                                request.getEmail()
                        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "이메일 인증번호 확인",
            description = "발송된 이메일 인증번호가 맞는지 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인증번호 확인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EmailVerificationConfirmResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "인증번호가 일치하지 않거나 만료됨")
    })
    // 이메일 인증번호 확인
    @PostMapping("/email/verification-code/confirm")
    public ResponseEntity<EmailVerificationConfirmResponse>
    confirmVerificationCode(
            @Valid
            @RequestBody
            EmailVerificationConfirmRequest request
    ) {
        EmailVerificationConfirmResponse response =
                emailVerificationService
                        .confirmVerificationCode(
                                request.getEmail(),
                                request.getCode()
                        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "비밀번호 찾기",
            description = "이메일과 이름으로 본인 확인 후, 비밀번호 재설정 링크(토큰)를 이메일로 발송합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 메일 발송 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PasswordFindResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음"),
            @ApiResponse(responseCode = "404", description = "일치하는 회원 정보가 없음")
    })
    // 비밀번호 찾기
    @PostMapping("/password/find")
    public ResponseEntity<PasswordFindResponse>
    findPassword(
            @Valid
            @RequestBody
            PasswordFindRequest request
    ) {
        PasswordFindResponse response =
                userService.findPassword(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = "비밀번호 찾기로 발급받은 재설정 토큰으로 새 비밀번호를 설정합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PasswordResetResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "새 비밀번호가 정책에 맞지 않거나 확인값이 일치하지 않음"),
            @ApiResponse(responseCode = "401", description = "재설정 토큰이 만료되었거나 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    // 비밀번호 재설정
    @PostMapping("/password/reset")
    public ResponseEntity<PasswordResetResponse>
    resetPassword(
            @Valid
            @RequestBody
            PasswordResetRequest request
    ) {
        PasswordResetResponse response =
                userService.resetPassword(request);

        return ResponseEntity.ok(response);
    }
}
