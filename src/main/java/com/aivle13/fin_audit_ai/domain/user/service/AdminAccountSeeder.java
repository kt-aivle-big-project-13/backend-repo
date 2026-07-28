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
import org.springframework.dao.DataIntegrityViolationException;
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
    private static final String DEFAULT_ADMIN_EMAIL = "admin@finaudit.ai";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (DEFAULT_ADMIN_EMAIL.equals(adminEmail)) {
            log.warn("ADMIN_EMAIL/ADMIN_PASSWORD 환경변수가 설정되지 않아 기본 관리자 계정({})을 사용합니다. "
                    + "운영 환경이라면 반드시 .env에 실제 값을 지정하세요.", DEFAULT_ADMIN_EMAIL);
        }

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

        // 여러 인스턴스가 동시에 기동해도 users.email의 unique 제약으로 한쪽만 성공하도록
        // 하고, 나머지는 "이미 시딩됨"으로 간주해 안전하게 넘어간다.
        try {
            userRepository.save(admin);
            userRepository.flush();
            log.info("관리자 계정 시딩 완료: {}", adminEmail);
        } catch (DataIntegrityViolationException e) {
            log.info("관리자 계정이 동시 기동 중인 다른 인스턴스에서 이미 생성되었습니다: {}", adminEmail);
        }
    }
}