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
import com.aivle13.fin_audit_ai.global.jwt.JwtProperties;
import com.aivle13.fin_audit_ai.global.jwt.JwtProvider;
import com.aivle13.fin_audit_ai.global.validation.PasswordValidator;
import com.aivle13.fin_audit_ai.global.exception.user.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    // 사용자 조회 및 저장
    @Mock
    private UserRepository userRepository;

    // 비밀번호 암호화 및 비교
    @Mock
    private PasswordEncoder passwordEncoder;

    // JWT 토큰 생성
    @Mock
    private JwtProvider jwtProvider;

    // JWT 설정값 관리
    @Mock
    private JwtProperties jwtProperties;

    // Refresh Token 저장 및 삭제
    @Mock
    private RefreshTokenService refreshTokenService;

    // 이메일 인증 상태 검증
    @Mock
    private EmailVerificationService emailVerificationService;

    // 비밀번호 유효성 검증
    @Mock
    private PasswordValidator passwordValidator;

    // reCAPTCHA 검증
    @Mock
    private RecaptchaService recaptchaService;

    // 테스트 대상 인증 서비스
    @InjectMocks
    private AuthService authService;

    @Test
    void signupSuccess() {
        // given: 회원가입 요청과 비밀번호 암호화 결과 설정
        SignupRequest request = new SignupRequest(
                " 에이블러 ",
                " 에이블스쿨 ",
                " TEST@EXAMPLE.COM ",
                "Password123!",
                "Password123!",
                true,
                true
        );

        given(userRepository.existsByEmail("test@example.com"))
                .willReturn(false);
        given(passwordEncoder.encode("Password123!"))
                .willReturn("encoded-password");

        // when: 회원가입 실행
        SignupResponse response = authService.signup(request);

        // then: 정규화된 회원 정보가 저장되었는지 검증
        ArgumentCaptor<UserEntity> userCaptor =
                ArgumentCaptor.forClass(UserEntity.class);

        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();

        assertThat(savedUser.getName()).isEqualTo("에이블러");
        assertThat(savedUser.getInstitution()).isEqualTo("에이블스쿨");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(response.message()).isEqualTo("회원가입이 완료되었습니다.");

        verify(emailVerificationService)
                .validateVerifiedEmail("test@example.com");
        verify(passwordValidator)
                .validate("Password123!", "Password123!");
        verify(passwordEncoder)
                .encode("Password123!");
        verify(emailVerificationService)
                .consumeVerification("test@example.com");
    }

    @Test
    void signupFailWhenEmailIsDuplicated() {
        // given: 이미 가입된 이메일로 회원가입 요청
        SignupRequest request = new SignupRequest(
                "에이블러",
                "에이블스쿨",
                " TEST@EXAMPLE.COM ",
                "Password123!",
                "Password123!",
                true,
                true
        );

        given(userRepository.existsByEmail("test@example.com"))
                .willReturn(true);

        // when & then: 중복 이메일 예외 발생 검증
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository)
                .existsByEmail("test@example.com");

        verify(emailVerificationService, never())
                .validateVerifiedEmail("test@example.com");

        verify(passwordValidator, never())
                .validate("Password123!", "Password123!");

        verify(passwordEncoder, never())
                .encode("Password123!");

        verify(userRepository, never())
                .save(any(UserEntity.class));

        verify(emailVerificationService, never())
                .consumeVerification("test@example.com");
    }

    @Test
    void loginSuccess() {
        // given: 활성 사용자의 로그인 정보와 토큰 발급 결과 설정
        LoginRequest request = new LoginRequest(
                " TEST@EXAMPLE.COM ",
                "Password123!",
                true,
                "recaptcha-token"
        );

        UserEntity user = mock(UserEntity.class);

        given(userRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(user));
        given(user.isActive())
                .willReturn(true);
        given(user.getPasswordHash())
                .willReturn("encoded-password");
        given(passwordEncoder.matches("Password123!", "encoded-password"))
                .willReturn(true);

        given(user.getId())
                .willReturn(1L);
        given(user.getRole())
                .willReturn(UserRole.USER);
        given(user.getEmail())
                .willReturn("test@example.com");
        given(user.getName())
                .willReturn("에이블러");

        given(jwtProvider.createAccessToken(1L, UserRole.USER))
                .willReturn("access-token");
        given(jwtProvider.createRefreshToken(1L, UserRole.USER, true))
                .willReturn("refresh-token");
        given(jwtProperties.accessTokenValidity())
                .willReturn(3_600_000L);

        // when: 로그인 실행
        TokenResponse response = authService.login(request);

        // then: 사용자 인증과 토큰 발급 결과 검증
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.name()).isEqualTo("에이블러");
        assertThat(response.role()).isEqualTo(UserRole.USER);

        verify(recaptchaService)
                .verify("recaptcha-token");
        verify(userRepository)
                .findByEmail("test@example.com");
        verify(passwordEncoder)
                .matches("Password123!", "encoded-password");
        verify(user)
                .updateLastLoginAt(any(LocalDateTime.class));
        verify(jwtProvider)
                .createAccessToken(1L, UserRole.USER);
        verify(jwtProvider)
                .createRefreshToken(1L, UserRole.USER, true);
        verify(refreshTokenService)
                .saveRefreshToken(1L, "refresh-token");
    }

    @Test
    void loginFailWhenPasswordIsInvalid() {
        // given: 존재하는 사용자의 비밀번호가 일치하지 않도록 설정
        LoginRequest request = new LoginRequest(
                " TEST@EXAMPLE.COM ",
                "WrongPassword123!",
                false,
                "recaptcha-token"
        );

        UserEntity user = mock(UserEntity.class);

        given(userRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(user));
        given(user.isActive())
                .willReturn(true);
        given(user.getPasswordHash())
                .willReturn("encoded-password");
        given(passwordEncoder.matches(
                "WrongPassword123!",
                "encoded-password"
        )).willReturn(false);

        // when & then: 로그인 실패 예외 발생 검증
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(recaptchaService)
                .verify("recaptcha-token");
        verify(userRepository)
                .findByEmail("test@example.com");
        verify(passwordEncoder)
                .matches("WrongPassword123!", "encoded-password");

        verify(user, never())
                .updateLastLoginAt(any(LocalDateTime.class));

        verifyNoInteractions(
                jwtProvider,
                jwtProperties,
                refreshTokenService
        );
    }
}