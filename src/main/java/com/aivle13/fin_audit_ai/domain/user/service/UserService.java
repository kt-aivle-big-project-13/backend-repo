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
import com.aivle13.fin_audit_ai.global.validation.PasswordValidator;
import com.aivle13.fin_audit_ai.global.util.EmailNormalizer;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenService tokenService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    public UserService(
            UserRepository userRepository,
            PasswordResetTokenService tokenService,
            MailService mailService,
            PasswordEncoder passwordEncoder,
            PasswordValidator passwordValidator
    ) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
    }

    // 비밀번호 찾기
    public PasswordFindResponse findPassword(
            PasswordFindRequest request
    ) {
        String email = EmailNormalizer.normalize(request.email());

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

        // 사용자에게 새로운 비밀번호 재설정 토큰 발급
        String token = tokenService.createToken(user.getId());

        // 발급된 토큰이 포함된 비밀번호 재설정 이메일 전송
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

        passwordValidator.validate(
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
                        new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 새 비밀번호를 BCrypt로 암호화
        String newPasswordHash = passwordEncoder.encode(newPassword);

        // 암호화된 비밀번호를 사용자 엔터티에 반영
        user.changePassword(newPasswordHash);

        return PasswordResetResponse.success();
    }

}