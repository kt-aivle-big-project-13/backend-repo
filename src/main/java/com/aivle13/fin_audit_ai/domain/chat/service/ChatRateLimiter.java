package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

/**
 * 사용자·감사별 일일 질문 수 제한.
 *
 * <p>질문 한 건마다 LLM 을 호출하므로 제한이 없으면 비용이 통제되지 않는다. 날짜를 키에
 * 넣어 자정에 자연히 초기화되게 하고, 키에 TTL 을 걸어 남은 카운터가 쌓이지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class ChatRateLimiter {

    private static final String KEY_PREFIX = "chat:quota:";
    private static final Duration KEY_TTL = Duration.ofDays(2);

    private final StringRedisTemplate redisTemplate;

    @Value("${app.chat.daily-question-limit:50}")
    private int dailyLimit;

    public void checkAndIncrease(Long userId, Long auditId) {
        String key = "%s%s:%d:%d".formatted(
                KEY_PREFIX,
                LocalDate.now(),
                userId,
                auditId
        );

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return;
        }

        // 첫 요청에서만 만료를 건다. 매번 걸면 하루 제한이 계속 뒤로 밀린다.
        if (count == 1L) {
            redisTemplate.expire(key, KEY_TTL);
        }

        if (count > dailyLimit) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
