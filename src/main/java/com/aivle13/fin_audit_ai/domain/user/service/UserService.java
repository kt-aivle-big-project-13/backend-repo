package com.aivle13.fin_audit_ai.domain.user.service;

import com.aivle13.fin_audit_ai.domain.user.dto.request.PasswordFindRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.PasswordResetRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.response.PasswordFindResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.PasswordResetResponse;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.mail.MailService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenService tokenService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordResetTokenService tokenService,
            MailService mailService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    public PasswordFindResponse findPassword(
            PasswordFindRequest request
    ) {
        UserEntity user = userRepository
                .findByEmailAndName(
                        request.email().trim(),
                        request.name().trim()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.PASSWORD_RESET_USER_NOT_FOUND
                        )
                );

        // 사용자에게 새로운 비밀번호 재설정 토큰 발급
        String token = tokenService.createToken(
                user.getId()
        );

        // 발급된 토큰이 포함된 비밀번호 재설정 이메일 전송
        mailService.sendPasswordResetMail(
                user.getEmail(),
                token
        );

        return PasswordFindResponse.success();
    }

    @Transactional
    public PasswordResetResponse resetPassword(
            PasswordResetRequest request
    ) {
        String token =
                request.resetToken().trim();

        String newPassword =
                request.newPassword();

        String newPasswordConfirm =
                request.newPasswordConfirm();

        // 새 비밀번호가 정책에 맞는지 검증
        validatePasswordPolicy(newPassword);

        // 새 비밀번호와 비밀번호 확인값이 같은지 검증
        validatePasswordConfirm(
                newPassword,
                newPasswordConfirm
        );

        // 토큰 조회와 삭제를 동시에 처리(consumeToken)
        // 토큰이 유효하면 사용자 ID가 반환. 만료됐거나 이미 사용된 토큰이면 null 반환.
        Long userId = tokenService.consumeToken(token);

        if (userId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_RESET_TOKEN
            );
        }

        // 토큰과 연결된 사용자 조회
        UserEntity user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );

        // 새 비밀번호를 BCrypt로 암호화
        String newPasswordHash =
                passwordEncoder.encode(newPassword);

        // 암호화된 비밀번호를 사용자 엔티티에 반영
        user.changePassword(newPasswordHash);

        return PasswordResetResponse.success();
    }

    // 새 비밀번호와 비밀번호 확인값의 일치 여부 검증
    private void validatePasswordConfirm(
            String newPassword,
            String newPasswordConfirm
    ) {
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_CONFIRM_NOT_MATCH
            );
        }
    }

    // 비밀번호 정책 검증
    private void validatePasswordPolicy(
            String password
    ) {
        boolean hasLetter = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        // 사용할 수 없는 특수문자
        String excludedCharacters = "()<>\"';";

        for (char character : password.toCharArray()) {
            // 공백, 출력 가능한 ASCII 범위 밖의 문자, 제한된 특수문자가 포함되면 검증 실패
            if (Character.isWhitespace(character)
                    || character < 33
                    || character > 126
                    || excludedCharacters
                    .indexOf(character) >= 0) {

                throw new BusinessException(
                        ErrorCode.INVALID_PASSWORD_POLICY
                );
            }

            if ((character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')) {
                hasLetter = true;
            } else if (character >= '0' && character <= '9') {
                hasNumber = true;
            } else {
                hasSpecial = true;
            }
        }

        // 비밀번호에 포함된 문자 종류 수 계산
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

        // 문자 종류가 2개라면 10~16자리 허용
        boolean validTwoTypes =
                typeCount == 2
                        && length >= 10
                        && length <= 16;

        // 문자 종류가 3개라면 8~16자리 허용
        boolean validThreeTypes =
                typeCount == 3
                        && length >= 8
                        && length <= 16;

        if (!validTwoTypes && !validThreeTypes) {
            throw new BusinessException(
                    ErrorCode.INVALID_PASSWORD_POLICY
            );
        }
    }
}