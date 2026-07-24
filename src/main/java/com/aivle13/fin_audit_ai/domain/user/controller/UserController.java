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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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