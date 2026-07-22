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

        String token = tokenService.createToken(
                user.getId()
        );

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
        String token = request.resetToken().trim();
        String newPassword = request.newPassword();
        String newPasswordConfirm =
                request.newPasswordConfirm();

        validatePasswordPolicy(newPassword);

        validatePasswordConfirm(
                newPassword,
                newPasswordConfirm
        );

        Long userId = tokenService.getUserId(token);

        if (userId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_RESET_TOKEN
            );
        }

        UserEntity user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );

        String newPasswordHash =
                passwordEncoder.encode(newPassword);

        user.changePassword(newPasswordHash);

        tokenService.deleteToken(token);

        return PasswordResetResponse.success();
    }

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

    private void validatePasswordPolicy(
            String password
    ) {
        boolean hasLetter = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        String excludedCharacters = "()<>\"';";

        for (char character : password.toCharArray()) {

            if (Character.isWhitespace(character)
                    || character < 33
                    || character > 126
                    || excludedCharacters.indexOf(character) >= 0) {

                throw new BusinessException(
                        ErrorCode.INVALID_PASSWORD_POLICY
                );
            }

            if (character >= 'A' && character <= 'Z'
                    || character >= 'a' && character <= 'z') {
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

        int length = password.length();

        boolean validTwoTypes =
                typeCount == 2
                        && length >= 10
                        && length <= 16;

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