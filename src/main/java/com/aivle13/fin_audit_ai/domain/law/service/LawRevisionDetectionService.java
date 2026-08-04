package com.aivle13.fin_audit_ai.domain.law.service;

import com.aivle13.fin_audit_ai.domain.law.entity.LawRevisionEntity;
import com.aivle13.fin_audit_ai.domain.notification.service.NotificationService;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * law.go.kr에서 추적 대상 법령의 현재 조문을 조회해, 우리 DB에 저장된 조문보다 시행일자가
 * 최신이면 개정으로 간주하고 반영한다. 법령 단위 실제 반영 작업은 {@link LawRevisionApplier}
 * 별도 빈에 위임한다 — 같은 클래스 안에서 self-invocation으로 호출하면 @Transactional이
 * 프록시를 우회해 적용되지 않기 때문이다. 법령·조문 단위로 실패를 격리해 하나가 실패해도
 * 나머지는 계속 처리한다 — {@link LawArticleEmbeddingService#embedMissingArticles()}와
 * 동일한 lenient 배치 패턴이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LawRevisionDetectionService {

    // law_articles.law_name(축약형) → law.go.kr 정식 법령명
    private static final Map<String, String> TRACKED_LAWS = Map.of(
            "AI 기본법", "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법",
            "AI 기본법 시행령", "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법 시행령"
    );

    private final LawRevisionApplier lawRevisionApplier;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<LawRevisionEntity> detectAndApply() {
        List<LawRevisionEntity> revisions = new ArrayList<>();

        for (Map.Entry<String, String> trackedLaw : TRACKED_LAWS.entrySet()) {
            try {
                revisions.addAll(lawRevisionApplier.applyForLaw(trackedLaw.getKey(), trackedLaw.getValue()));
            } catch (RuntimeException exception) {
                log.error(
                        "법령 개정 감지 실패, 다음 추적 대상 법령으로 계속함: lawName={}",
                        trackedLaw.getKey(),
                        exception
                );
            }
        }

        if (!revisions.isEmpty()) {
            notifyActiveUsers(revisions);
        }

        return revisions;
    }

    // 개정 알림은 감사(트랜잭션)와 무관한 브로드캐스트라 모든 법령 처리(및 커밋)가 끝난
    // 뒤에 트랜잭션 밖에서 실행한다 — NotificationService.notifyLawRevisions도 메일 발송을
    // DB 트랜잭션 밖에서 수행하도록 설계돼 있어 이 순서와 맞는다.
    // 한 배치에서 감지된 개정 건을 사용자당 한 통으로 모아 보내야 해서, 조문이 아니라
    // 사용자 기준으로 순회한다 — 이전엔 조문×사용자 이중 루프라 조문 수만큼 메일이 갔다.
    private void notifyActiveUsers(List<LawRevisionEntity> revisions) {
        List<UserEntity> activeUsers = userRepository.findByIsActiveTrue();

        for (UserEntity user : activeUsers) {
            try {
                notificationService.notifyLawRevisions(user, revisions);
            } catch (RuntimeException exception) {
                log.error(
                        "법령 개정 알림 발송 실패, 다음 사용자로 계속함: userId={}",
                        user.getId(),
                        exception
                );
            }
        }
    }
}
