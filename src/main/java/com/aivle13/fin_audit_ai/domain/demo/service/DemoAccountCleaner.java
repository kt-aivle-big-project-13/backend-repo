package com.aivle13.fin_audit_ai.domain.demo.service;

import com.aivle13.fin_audit_ai.domain.demo.config.DemoProperties;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 만료된 게스트 계정 정리.
 *
 * <p>방문자마다 계정과 데모 데이터가 생기므로 두면 계속 쌓인다. 시연이 끝난 계정은 다시
 * 쓰이지 않으니 만료된 것부터 지운다.
 *
 * <p>한 번에 지우는 개수를 제한한다. 오래 쌓인 뒤 처음 도는 경우 한 트랜잭션이 지나치게
 * 길어지는 것을 막기 위해서다. 남은 것은 다음 주기에 지워진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoAccountCleaner {

    private static final int BATCH_SIZE = 200;

    private final DemoProperties demoProperties;
    private final UserRepository userRepository;
    private final DemoGuestPurger guestPurger;

    @Scheduled(cron = "${app.demo.cleanup-cron:0 0 * * * *}")
    public void cleanExpiredGuests() {
        if (!demoProperties.enabled()) {
            return;
        }

        LocalDateTime expiredBefore =
                LocalDateTime.now().minus(demoProperties.guestTtl());

        List<Long> expired = userRepository.findExpiredGuestIds(
                DemoAccountService.GUEST_EMAIL_PREFIX + "%" + DemoAccountService.GUEST_EMAIL_DOMAIN,
                expiredBefore,
                Limit.of(BATCH_SIZE)
        );

        guestPurger.purge(expired);
    }
}
