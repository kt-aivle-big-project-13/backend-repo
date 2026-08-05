package com.aivle13.fin_audit_ai.domain.user.service;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class LoadTestAccountSeeder implements ApplicationRunner {

    private static final String LOAD_TEST_NAME = "에이블러";
    private static final String LOAD_TEST_INSTITUTION = "FinAuditAI";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${loadtest.user.email}")
    private String loadTestEmail;

    @Value("${loadtest.user.password}")
    private String loadTestPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(loadTestEmail)) {
            return;
        }

        UserEntity user = UserEntity.create(
                LOAD_TEST_NAME,
                LOAD_TEST_INSTITUTION,
                loadTestEmail,
                passwordEncoder.encode(loadTestPassword),
                UserRole.USER
        );

        try {
            userRepository.save(user);
            userRepository.flush();
            log.info("부하 테스트 계정 시딩 완료: {}", loadTestEmail);
        } catch (DataIntegrityViolationException e) {
            log.info("부하 테스트 계정이 이미 생성되어 있습니다: {}", loadTestEmail);
        }
    }
}