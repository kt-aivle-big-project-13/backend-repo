package com.aivle13.fin_audit_ai.domain.auth.service;

import com.aivle13.fin_audit_ai.domain.auth.dto.request.LoginRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.request.SignupRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.response.SignupResponse;
import com.aivle13.fin_audit_ai.domain.auth.dto.response.TokenResponse;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.user.DuplicateEmailException;
import com.aivle13.fin_audit_ai.global.exception.user.InvalidCredentialsException;
import com.aivle13.fin_audit_ai.global.exception.user.UserNotFoundException;
import com.aivle13.fin_audit_ai.global.jwt.JwtProperties;
import com.aivle13.fin_audit_ai.global.jwt.JwtProvider;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            JwtProperties jwtProperties,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = request.email().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }

        UserEntity user = UserEntity.create(
                request.institution().trim(),
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.USER
        );

        userRepository.save(user);

        return SignupResponse.from(user);
    }

    public TokenResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    // 리프레시 토큰 롤링: 서명/만료 검증 -> Redis 저장값과 일치 검증 -> 통과 시 access/refresh 모두 새로 발급하고 Redis 값 교체
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        jwtProvider.validateRefreshToken(refreshToken);
        Long userId = jwtProvider.getUserId(refreshToken);

        refreshTokenService.validateStoredToken(userId, refreshToken);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return issueTokens(user);
    }

    public void logout(Long userId, String accessToken) {
        refreshTokenService.revoke(userId, accessToken);
    }

    private TokenResponse issueTokens(UserEntity user) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getRole());

        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

        long expiresIn = jwtProperties.accessTokenValidity() / 1000;

        return TokenResponse.of(accessToken, refreshToken, expiresIn);
    }
}