package com.aivle13.fin_audit_ai.domain.user.service;

import com.aivle13.fin_audit_ai.domain.user.dto.request.notification.UpdateNotificationPreferencesRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.password.ChangePasswordRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.password.PasswordFindRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.password.PasswordResetRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.password.VerifyPasswordRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.profile.UpdateNameRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.request.withdraw.WithdrawRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.ChangePasswordResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.PasswordFindResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.PasswordResetResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.password.VerifyPasswordResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.profile.UserResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.withdraw.WithdrawResponse;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.service.password.PasswordResetTokenService;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.user.common.UserNotFoundException;
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

    // 나의 프로필 조회
    public UserResponse getMyProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return UserResponse.from(user);
    }

    // 회원정보(이름) 수정
    @Transactional
    public UserResponse updateMyProfile(Long userId, UpdateNameRequest request) {
        String name = request.name();

        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.NO_UPDATABLE_FIELD);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        user.updateName(name.trim());

        return UserResponse.from(user);
    }

    // 마이페이지 알림 설정 저장
    @Transactional
    public UserResponse updateNotificationPreferences(Long userId, UpdateNotificationPreferencesRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        user.updateNotificationPreferences(
                request.lawEmailEnabled(),
                request.reauditAlertEnabled(),
                request.auditCompleteAlertEnabled(),
                request.auditFailAlertEnabled()
        );

        return UserResponse.from(user);
    }

    // 마이페이지 비밀번호 변경 전 현재 비밀번호 확인
    public VerifyPasswordResponse verifyCurrentPassword(Long userId, VerifyPasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }

        return VerifyPasswordResponse.success();
    }

    // 마이페이지 비밀번호 변경
    @Transactional
    public ChangePasswordResponse changeMyPassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }

        passwordValidator.validate(request.newPassword(), request.newPasswordConfirm());

        user.changePassword(passwordEncoder.encode(request.newPassword()));

        return ChangePasswordResponse.success();
    }

    // 회원 탈퇴 (소프트 삭제) - 파괴적 동작이라 비밀번호 재확인 후 처리
    @Transactional
    public WithdrawResponse withdraw(Long userId, WithdrawRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }

        user.withdraw();

        return WithdrawResponse.success();
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

        String token = tokenService.createToken(user.getId());

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

        Long userId = tokenService.consumeToken(token);

        if (userId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_RESET_TOKEN
            );
        }

        UserEntity user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newPasswordHash = passwordEncoder.encode(newPassword);

        user.changePassword(newPasswordHash);

        return PasswordResetResponse.success();
    }

}