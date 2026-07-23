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

import java.util.Locale;

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

    // 비밀번호 찾기
    public PasswordFindResponse findPassword(
            PasswordFindRequest request
    ) {
        String email =
                normalizeEmail(request.email());

        UserEntity user = userRepository
                .findByEmailAndName(
                        email,
                        request.name().trim()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.PASSWORD_RESET_USER_NOT_FOUND
                        )
                );

        String token =
                tokenService.createToken(
                        user.getId()
                );

        mailService.sendPasswordResetMail(
                user.getEmail(),
                token
        );

        return PasswordFindResponse.success();
    }

    // 비밀번호 재설정
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

        validatePasswordPolicy(newPassword);

        validatePasswordConfirm(
                newPassword,
                newPasswordConfirm
        );

        Long id =
                tokenService.consumeToken(token);

        if (id == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_RESET_TOKEN
            );
        }

        UserEntity user = userRepository
                .findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );

        String newPasswordHash =
                passwordEncoder.encode(newPassword);

        user.changePassword(newPasswordHash);

        return PasswordResetResponse.success();
    }

    // 비밀번호 확인값 일치 여부 검증
    private void validatePasswordConfirm(
            String password,
            String passwordConfirm
    ) {
        if (!password.equals(passwordConfirm)) {
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

        String excludedCharacters =
                "()<>\"';";

        for (char character : password.toCharArray()) {

            if (Character.isWhitespace(character)
                    || character < 33
                    || character > 126
                    || excludedCharacters.indexOf(character) >= 0) {

                throw new BusinessException(
                        ErrorCode.INVALID_PASSWORD_POLICY
                );
            }

            if ((character >= 'A' && character <= 'Z')
                    || (character >= 'a'
                    && character <= 'z')) {

                hasLetter = true;

            } else if (character >= '0'
                    && character <= '9') {

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

        int length =
                password.length();

        boolean validTwoTypes =
                typeCount == 2
                        && length >= 10
                        && length <= 16;

        boolean validThreeTypes =
                typeCount == 3
                        && length >= 8
                        && length <= 16;

        if (!validTwoTypes
                && !validThreeTypes) {

            throw new BusinessException(
                    ErrorCode.INVALID_PASSWORD_POLICY
            );
        }
    }

    private String normalizeEmail(
            String email
    ) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}