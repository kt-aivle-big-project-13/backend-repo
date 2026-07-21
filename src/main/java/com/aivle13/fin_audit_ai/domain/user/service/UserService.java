package com.aivle13.fin_audit_ai.domain.user.service;

import com.aivle13.fin_audit_ai.domain.user.dto.request.PasswordFindRequest;
import com.aivle13.fin_audit_ai.domain.user.dto.response.PasswordFindResponse;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.mail.MailService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenService tokenService;
    private final MailService mailService;

    public UserService(
            UserRepository userRepository,
            PasswordResetTokenService tokenService,
            MailService mailService
    ) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.mailService = mailService;
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
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "일치하는 회원 정보 없음"
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
}