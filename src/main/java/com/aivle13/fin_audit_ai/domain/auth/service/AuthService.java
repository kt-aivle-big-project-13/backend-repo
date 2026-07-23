package com.aivle13.fin_audit_ai.domain.auth.service;

import com.aivle13.fin_audit_ai.domain.auth.dto.request.LoginRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.request.SignupRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.response.SignupResponse;
import com.aivle13.fin_audit_ai.domain.auth.dto.response.TokenResponse;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.service.EmailVerificationService;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.user.DuplicateEmailException;
import com.aivle13.fin_audit_ai.global.exception.user.InvalidCredentialsException;
import com.aivle13.fin_audit_ai.global.exception.user.UserNotFoundException;
import com.aivle13.fin_audit_ai.global.jwt.JwtProperties;
import com.aivle13.fin_audit_ai.global.jwt.JwtProvider;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            JwtProperties jwtProperties,
            RefreshTokenService refreshTokenService,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());

        String password = request.password();
        String passwordConfirm = request.passwordConfirm();

        // 이미 사용 중인 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        emailVerificationService.validateVerifiedEmail(email);

        // 비밀번호 정책 검증
        validatePasswordPolicy(password);

        // 비밀번호와 확인값 일치 여부 검증
        validatePasswordConfirm(password, passwordConfirm);

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

    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

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

    // 비밀번호 확인값 일치 여부 검증
    private void validatePasswordConfirm(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }
    }

    // 비밀번호 정책 검증
    private void validatePasswordPolicy(String password) {
        boolean hasLetter = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        String excludedCharacters = "()<>\"';";

        for (char character : password.toCharArray()) {
            if (Character.isWhitespace(character)
                    || character < 33
                    || character > 126
                    || excludedCharacters.indexOf(character) >= 0) {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD_POLICY);
            }

            if ((character >= 'A' && character <= 'Z')
                    || (character >= 'a' && character <= 'z')) {
                hasLetter = true;
            } else if (character >= '0' && character <= '9') {
                hasNumber = true;
            } else {
                hasSpecial = true;
            }
        }

        int typeCount = 0;

        if (hasLetter) {
            typeCount++;
        }

        if (hasNumber) {
            typeCount++;
        }

        if (hasSpecial) {
            typeCount++;
        }

        int length = password.length();

        boolean validTwoTypes = typeCount == 2
                && length >= 10
                && length <= 16;

        boolean validThreeTypes = typeCount == 3
                && length >= 8
                && length <= 16;

        if (!validTwoTypes && !validThreeTypes) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_POLICY);
        }
    }

    private TokenResponse issueTokens(UserEntity user, boolean rememberMe) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getRole(), rememberMe);

        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

        long expiresIn = jwtProperties.accessTokenValidity() / 1000;

        return TokenResponse.of(accessToken, refreshToken, expiresIn, user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}