package com.aivle13.fin_audit_ai.domain.user.service;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 관리자 계정을 기동 시 보장한다. 로그인은 이메일 기반으로만 동작하므로
 * "admin" 아이디 요구사항은 이메일(app.admin.email)로 대체한다.
 * 이미 존재하면 아무 작업도 하지 않아 재기동해도 안전하다.
 */
@Slf4j
@Component
@Order(-1)
@RequiredArgsConstructor
public class AdminAccountSeeder implements ApplicationRunner {

    private static final String ADMIN_NAME = "관리자";
    private static final String ADMIN_INSTITUTION = "FinAuditAI";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        UserEntity admin = UserEntity.create(
                ADMIN_NAME,
                ADMIN_INSTITUTION,
                adminEmail,
                passwordEncoder.encode(adminPassword),
                UserRole.ADMIN
        );

        userRepository.save(admin);

        log.info("관리자 계정 시딩 완료: {}", adminEmail);
    }
}