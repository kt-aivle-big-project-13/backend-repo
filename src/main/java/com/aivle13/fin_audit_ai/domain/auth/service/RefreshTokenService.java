package com.aivle13.fin_audit_ai.domain.auth.service;

import com.aivle13.fin_audit_ai.global.exception.user.RefreshTokenMismatchException;
import com.aivle13.fin_audit_ai.global.jwt.JwtProperties;
import com.aivle13.fin_audit_ai.global.jwt.JwtProvider;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenService {

    // 사용자 ID -> 현재 유효한 리프레시 토큰 (1인당 1개, 재발급마다 덮어써짐 = 롤링)
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";

    // 로그아웃된 액세스 토큰 -> "logout" (잔여 수명만큼만 TTL)
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            JwtProvider jwtProvider,
            JwtProperties jwtProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
    }

    // 로그인/재발급 시 새 리프레시 토큰 저장. 기존 값은 덮어써지며 자동 폐기(롤링).
    public void saveRefreshToken(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(jwtProperties.refreshTokenValidity())
        );
    }

    // 재발급 요청 시 저장된 토큰과 일치하는지 검증. 불일치하거나 이미 폐기된 경우 탈취 의심으로 보고 세션을 강제 종료.
    public void validateStoredToken(Long userId, String refreshToken) {
        String storedToken = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
            throw new RefreshTokenMismatchException();
        }
    }

    // 로그아웃: 리프레시 세션 삭제 + 현재 액세스 토큰을 잔여 수명만큼 블랙리스트에 등록
    public void revoke(Long userId, String accessToken) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);

        long remaining = jwtProvider.getRemainingValidity(accessToken);

        if (remaining > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_KEY_PREFIX + accessToken,
                    "logout",
                    Duration.ofMillis(remaining)
            );
        }
    }
}