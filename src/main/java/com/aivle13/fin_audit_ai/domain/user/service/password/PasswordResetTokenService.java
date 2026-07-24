package com.aivle13.fin_audit_ai.domain.user.service.password;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetTokenService {

    // 토큰 -> 사용자 ID
    private static final String TOKEN_KEY_PREFIX = "password-reset:token:";

    // 사용자 ID -> 현재 발급된 토큰
    private static final String USER_KEY_PREFIX = "password-reset:user:";

    // 비밀번호 재설정 토큰 유효시간
    private static final Duration EXPIRATION = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public PasswordResetTokenService(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    // 비밀번호 재설정 토큰 생성
    public String createToken(Long userId) {
        String userKey = USER_KEY_PREFIX + userId;

        // 사용자에게 기존에 발급된 토큰 조회
        String oldToken = redisTemplate.opsForValue().get(userKey);

        // 기존 토큰이 존재 시 토큰 키 삭제
        if (oldToken != null) {
            redisTemplate.delete(
                    TOKEN_KEY_PREFIX + oldToken
            );
        }

        // 새로운 일회용 토큰 생성
        String newToken =
                UUID.randomUUID().toString();

        // 토큰 -> 사용자 ID 저장
        redisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + newToken,
                userId.toString(),
                EXPIRATION
        );

        // 사용자 ID -> 현재 토큰 저장
        redisTemplate.opsForValue().set(
                userKey,
                newToken,
                EXPIRATION
        );

        return newToken;
    }

    // 토큰을 조회하고 동시에 삭제
    public Long consumeToken(String token) {
        String tokenKey =
                TOKEN_KEY_PREFIX + token;

        // 동일 토큰의 중복 사용 방지
        String userId =
                redisTemplate
                        .opsForValue()
                        .getAndDelete(tokenKey);

        if (userId == null) {
            return null;
        }

        String userKey = USER_KEY_PREFIX + userId;

        String currentToken =
                redisTemplate
                        .opsForValue()
                        .get(userKey);

        // 현재 연결된 토큰인 경우 사용자 키도 삭제
        if (token.equals(currentToken)) {
            redisTemplate.delete(userKey);
        }

        return Long.valueOf(userId);
    }
}