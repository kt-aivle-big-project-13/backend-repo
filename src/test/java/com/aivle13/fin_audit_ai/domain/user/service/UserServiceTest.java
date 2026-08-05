package com.aivle13.fin_audit_ai.domain.user.service;

import com.aivle13.fin_audit_ai.domain.user.dto.request.password.PasswordFindRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.PasswordFindResponse;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.service.password.PasswordResetTokenService;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.mail.MailService;
import com.aivle13.fin_audit_ai.global.validation.PasswordValidator;
import com.aivle13.fin_audit_ai.domain.user.dto.request.password.PasswordResetRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.PasswordResetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    // 사용자 조회 및 저장
    @Mock
    private UserRepository userRepository;

    // 비밀번호 재설정 토큰 생성 및 사용
    @Mock
    private PasswordResetTokenService tokenService;

    // 비밀번호 재설정 메일 발송
    @Mock
    private MailService mailService;

    // 비밀번호 암호화 및 비교
    @Mock
    private PasswordEncoder passwordEncoder;

    // 비밀번호 유효성 검증
    @Mock
    private PasswordValidator passwordValidator;

    // 테스트 대상 사용자 서비스
    @InjectMocks
    private UserService userService;

    @Test
    void findPasswordSuccess() {
        // given: 이름과 이메일이 일치하는 사용자와 재설정 토큰 설정
        PasswordFindRequest request = new PasswordFindRequest(
                " 에이블러 ",
                " TEST@EXAMPLE.COM "
        );

        UserEntity user = mock(UserEntity.class);

        given(userRepository.findByEmailAndName(
                "test@example.com",
                "에이블러"
        )).willReturn(Optional.of(user));

        given(user.getId())
                .willReturn(1L);
        given(user.getEmail())
                .willReturn("test@example.com");
        given(tokenService.createToken(1L))
                .willReturn("reset-token");

        // when: 비밀번호 찾기 실행
        PasswordFindResponse response =
                userService.findPassword(request);

        // then: 토큰 생성과 비밀번호 재설정 메일 발송 검증
        assertThat(response.message())
                .isEqualTo("비밀번호 재설정 링크가 이메일로 발송되었습니다.");
        assertThat(response.expiresIn())
                .isEqualTo(1800L);

        verify(userRepository)
                .findByEmailAndName(
                        "test@example.com",
                        "에이블러"
                );

        verify(tokenService)
                .createToken(1L);

        verify(mailService)
                .sendPasswordResetMail(
                        "test@example.com",
                        "reset-token"
                );
    }

    @Test
    void findPasswordFailWhenUserDoesNotMatch() {
        // given: 이름과 이메일이 일치하는 사용자가 없도록 설정
        PasswordFindRequest request = new PasswordFindRequest(
                " 에이블러 ",
                " TEST@EXAMPLE.COM "
        );

        given(userRepository.findByEmailAndName(
                "test@example.com",
                "에이블러"
        )).willReturn(Optional.empty());

        // when: 비밀번호 찾기 실행
        BusinessException exception = catchThrowableOfType(
                () -> userService.findPassword(request),
                BusinessException.class
        );

        // then: 사용자 불일치 예외와 후속 로직 미실행 검증
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_RESET_USER_NOT_FOUND);

        verify(userRepository)
                .findByEmailAndName(
                        "test@example.com",
                        "에이블러"
                );

        verify(tokenService, never())
                .createToken(any(Long.class));

        verify(mailService, never())
                .sendPasswordResetMail(
                        any(String.class),
                        any(String.class)
                );
    }

    @Test
    void resetPasswordSuccess() {
        // given: 유효한 재설정 토큰과 새 비밀번호 설정
        PasswordResetRequest request = new PasswordResetRequest(
                "reset-token",
                "NewPassword123!",
                "NewPassword123!"
        );

        UserEntity user = mock(UserEntity.class);

        given(tokenService.consumeToken("reset-token"))
                .willReturn(1L);
        given(userRepository.findById(1L))
                .willReturn(Optional.of(user));
        given(passwordEncoder.encode("NewPassword123!"))
                .willReturn("new-encoded-password");

        // when: 비밀번호 재설정 실행
        PasswordResetResponse response =
                userService.resetPassword(request);

        // then: 새 비밀번호 검증 및 변경 결과 확인
        assertThat(response.message())
                .isEqualTo("비밀번호가 변경되었습니다.");

        verify(passwordValidator)
                .validate(
                        "NewPassword123!",
                        "NewPassword123!"
                );

        verify(tokenService)
                .consumeToken("reset-token");

        verify(userRepository)
                .findById(1L);

        verify(passwordEncoder)
                .encode("NewPassword123!");

        verify(user)
                .changePassword("new-encoded-password");
    }

    @Test
    void resetPasswordFailWhenTokenIsInvalid() {
        // given: 유효하지 않은 재설정 토큰과 새 비밀번호 설정
        PasswordResetRequest request = new PasswordResetRequest(
                "invalid-reset-token",
                "NewPassword123!",
                "NewPassword123!"
        );

        given(tokenService.consumeToken("invalid-reset-token"))
                .willReturn(null);

        // when: 비밀번호 재설정 실행
        BusinessException exception = catchThrowableOfType(
                () -> userService.resetPassword(request),
                BusinessException.class
        );

        // then: 유효하지 않은 토큰 예외와 후속 로직 미실행 검증
        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.INVALID_RESET_TOKEN);

        verify(passwordValidator)
                .validate(
                        "NewPassword123!",
                        "NewPassword123!"
                );

        verify(tokenService)
                .consumeToken("invalid-reset-token");

        verify(userRepository, never())
                .findById(any(Long.class));

        verify(passwordEncoder, never())
                .encode(any(String.class));
    }
}