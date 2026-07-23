package com.aivle13.fin_audit_ai.domain.auth.controller;

import com.aivle13.fin_audit_ai.domain.auth.dto.request.LoginRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.request.ReissueRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.request.SignupRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.response.SignupResponse;
import com.aivle13.fin_audit_ai.domain.auth.dto.response.TokenResponse;
import com.aivle13.fin_audit_ai.domain.auth.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @Valid @RequestBody ReissueRequest request
    ) {
        TokenResponse response = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    // 인증 필요(퍼블릭 엔드포인트 아님) - JwtAuthenticationFilter가 세팅한 principal/credentials 사용
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String accessToken = (String) authentication.getCredentials();

        authService.logout(userId, accessToken);

        return ResponseEntity.noContent().build();
    }
}