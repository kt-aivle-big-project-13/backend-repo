package com.aivle13.fin_audit_ai.domain.demo.service;

import com.aivle13.fin_audit_ai.domain.auth.dto.response.TokenResponse;
import com.aivle13.fin_audit_ai.domain.auth.service.AuthService;
import com.aivle13.fin_audit_ai.domain.demo.config.DemoProperties;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 시연용 게스트 계정 발급.
 *
 * <p>방문자마다 계정을 새로 만든다. 계정 하나를 공유하면 서로의 감사 이력이 다 보이고,
 * 감사 진행 중 판정이 모델 단위라 한 명이 감사를 돌리는 동안 나머지가 시작하지 못한다.
 *
 * <p>비밀번호는 임의값을 넣고 알려주지 않는다. 이 계정은 발급 시 받은 토큰으로만 쓰는
 * 일회용이고, 비밀번호로 다시 로그인할 수 있으면 정리된 뒤에도 흔적이 남는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoAccountService {

    private static final String GUEST_NAME = "게스트";
    private static final String GUEST_INSTITUTION = "시연";

    // 정리 대상을 찾는 기준이자, 실제 사용자와 섞이지 않게 하는 표시다.
    static final String GUEST_EMAIL_PREFIX = "guest-";
    static final String GUEST_EMAIL_DOMAIN = "@demo.invalid";

    private final DemoProperties demoProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoDataProvisioner demoDataProvisioner;
    private final AuthService authService;

    @Transactional
    public TokenResponse issueGuest() {
        if (!demoProperties.enabled()) {
            // 켜져 있지 않은 환경에서는 이 경로가 존재하지 않는 것처럼 보이게 한다.
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        UserEntity guest = userRepository.save(createGuest());

        demoDataProvisioner.provision(guest);

        log.info("시연용 게스트 계정 발급: userId={}", guest.getId());

        return authService.issueTokensWithoutCredentials(guest);
    }

    private UserEntity createGuest() {
        String email = GUEST_EMAIL_PREFIX
                + UUID.randomUUID().toString().replace("-", "")
                + GUEST_EMAIL_DOMAIN;

        return UserEntity.create(
                GUEST_NAME,
                GUEST_INSTITUTION,
                email,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                UserRole.USER
        );
    }
}
