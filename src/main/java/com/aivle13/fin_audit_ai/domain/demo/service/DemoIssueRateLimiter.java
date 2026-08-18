package com.aivle13.fin_audit_ai.domain.demo.service;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 게스트 발급 횟수 제한.
 *
 * <p>인증 없이 열린 경로인데 요청 한 번마다 계정·모델·데이터셋·감사 결과·이의제기와
 * 리프레시 토큰이 생긴다. 제한이 없으면 반복 호출만으로 DB 를 채울 수 있다.
 *
 * <p>기준은 호출자 IP 다. 로그인 전이라 사용자 단위로 셀 수 없다. NAT 뒤의 여러 명이 한
 * IP 로 묶일 수 있어 한 사람이 여러 번 누르는 것보다 넉넉하게 잡는다.
 */
@Component
@RequiredArgsConstructor
public class DemoIssueRateLimiter {

    private static final String KEY_PREFIX = "demo:issue:";
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.demo.issue-limit-per-hour:20}")
    private int issueLimitPerHour;

    public void checkAndIncrease(HttpServletRequest request) {
        String key = KEY_PREFIX + clientIp(request);

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return;
        }

        // 첫 요청에서만 만료를 건다. 매번 걸면 창이 계속 뒤로 밀려 제한이 풀리지 않는다.
        if (count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }

        if (count > issueLimitPerHour) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    /**
     * ALB 뒤에 있어 {@code getRemoteAddr()} 는 로드밸런서 주소가 된다.
     *
     * <p>{@code X-Forwarded-For} 는 클라이언트가 위조할 수 있지만, 위조해도 자기 몫의
     * 카운터만 갈라질 뿐 다른 사람을 막지는 못한다. 시연 환경의 남용을 늦추는 것이
     * 목적이라 이 정도로 둔다.
     */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);

        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
