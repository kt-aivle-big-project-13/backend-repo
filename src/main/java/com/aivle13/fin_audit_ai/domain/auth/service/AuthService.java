package com.aivle13.fin_audit_ai.domain.auth.service;

import com.aivle13.fin_audit_ai.domain.auth.dto.request.LoginRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.request.SignupRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.response.SignupResponse;
import com.aivle13.fin_audit_ai.domain.auth.dto.response.TokenResponse;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.service.email.EmailVerificationService;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.user.DuplicateEmailException;
import com.aivle13.fin_audit_ai.global.exception.user.InvalidCredentialsException;
import com.aivle13.fin_audit_ai.global.exception.user.UserNotFoundException;
import com.aivle13.fin_audit_ai.global.jwt.JwtProperties;
import com.aivle13.fin_audit_ai.global.jwt.JwtProvider;
import com.aivle13.fin_audit_ai.global.validation.PasswordValidator;
import com.aivle13.fin_audit_ai.global.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordValidator passwordValidator;
    private final RecaptchaService recaptchaService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = EmailNormalizer.normalize(request.email());

        String password = request.password();
        String passwordConfirm = request.passwordConfirm();

        // 이미 사용 중인 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        emailVerificationService.validateVerifiedEmail(email);

        passwordValidator.validate(
                password,
                passwordConfirm
        );

        // 비밀번호 BCrypt 암호화
        String passwordHash = passwordEncoder.encode(password);

        // 회원 엔티티 생성
        UserEntity user = UserEntity.create(
                request.name().trim(),
                request.institution().trim(),
                email,
                passwordHash,
                UserRole.USER
        );

        userRepository.save(user);

        emailVerificationService.consumeVerification(email);

        return SignupResponse.success();
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        recaptchaService.verify(request.recaptchaToken());

        String email = EmailNormalizer.normalize(request.email());

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        // 탈퇴한 계정인지 여부는 노출하지 않고, 다른 로그인 실패와 동일하게 처리한다.
        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.updateLastLoginAt(java.time.LocalDateTime.now(ZoneId.of("Asia/Seoul")));

        return issueTokens(user, request.isRememberMe());
    }

    // 리프레시 토큰 롤링: 서명/만료 검증 -> Redis 저장값과 일치 검증 -> 통과 시 access/refresh 모두 새로 발급하고 Redis 값 교체
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        jwtProvider.validateRefreshToken(refreshToken);

        Long userId = jwtProvider.getUserId(refreshToken);
        boolean rememberMe = jwtProvider.getRememberMe(refreshToken);

        refreshTokenService.validateStoredToken(userId, refreshToken);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return issueTokens(user, rememberMe);
    }

    public void logout(Long userId, String accessToken) {
        refreshTokenService.revoke(userId, accessToken);
    }

    private TokenResponse issueTokens(UserEntity user, boolean rememberMe) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getRole(), rememberMe);

        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

        long expiresIn = jwtProperties.accessTokenValidity() / 1000;

        return TokenResponse.of(accessToken, refreshToken, expiresIn, user);
    }
}