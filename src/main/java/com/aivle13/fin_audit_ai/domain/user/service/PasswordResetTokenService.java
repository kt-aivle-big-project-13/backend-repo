package com.aivle13.fin_audit_ai.domain.user.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetTokenService {

    // 토큰으로 사용자 ID를 조회하기 위한 Redis 키
    private static final String TOKEN_KEY_PREFIX =
            "password-reset:token:";

    // 사용자에게 현재 발급된 토큰을 조회하기 위한 Redis 키
    private static final String USER_KEY_PREFIX =
            "password-reset:user:";

    // 비밀번호 재설정 토큰 유효시간
    private static final Duration EXPIRATION =
            Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public PasswordResetTokenService(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 비밀번호 재설정 토큰 생성
     *
     * 같은 사용자가 이전에 발급받은 토큰이 있으면
     * 기존 토큰을 먼저 삭제한 뒤 새로운 토큰을 발급한다.
     */
    public String createToken(Long userId) {
        String userKey =
                USER_KEY_PREFIX + userId;

        // 해당 사용자에게 기존에 발급된 토큰 조회
        String oldToken =
                redisTemplate.opsForValue().get(userKey);

        // 기존 토큰이 있으면 무효화
        if (oldToken != null) {
            redisTemplate.delete(
                    TOKEN_KEY_PREFIX + oldToken
            );
        }

        // 새로운 일회성 토큰 생성
        String newToken =
                UUID.randomUUID().toString();

        // 토큰 → 사용자 ID 저장
        redisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + newToken,
                userId.toString(),
                EXPIRATION
        );

        // 사용자 ID → 현재 토큰 저장
        redisTemplate.opsForValue().set(
                userKey,
                newToken,
                EXPIRATION
        );

        return newToken;
    }

    /**
     * 토큰으로 사용자 ID 조회
     *
     * 토큰이 존재하지 않거나 만료된 경우 null을 반환한다.
     */
    public Long getUserId(String token) {
        String userId =
                redisTemplate.opsForValue().get(
                        TOKEN_KEY_PREFIX + token
                );

        if (userId == null) {
            return null;
        }

        return Long.valueOf(userId);
    }

    /**
     * 사용이 완료된 토큰 삭제
     *
     * 비밀번호 변경 성공 후 호출하면
     * 해당 토큰을 다시 사용할 수 없게 된다.
     */
    public void deleteToken(String token) {
        String tokenKey =
                TOKEN_KEY_PREFIX + token;

        String userId =
                redisTemplate.opsForValue().get(tokenKey);

        // 토큰 키 삭제
        redisTemplate.delete(tokenKey);

        if (userId == null) {
            return;
        }

        String userKey =
                USER_KEY_PREFIX + userId;

        String currentToken =
                redisTemplate.opsForValue().get(userKey);

        // 현재 사용자에게 연결된 토큰이 삭제하려는 토큰과 같은 경우에만 사용자 키 삭제
        if (token.equals(currentToken)) {
            redisTemplate.delete(userKey);
        }
    }
}