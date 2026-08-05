package com.aivle13.fin_audit_ai.domain.notification.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.law.entity.LawRevisionEntity;
import com.aivle13.fin_audit_ai.domain.notification.dto.response.NotificationResponse;
import com.aivle13.fin_audit_ai.domain.notification.entity.NotificationEntity;
import com.aivle13.fin_audit_ai.domain.notification.repository.NotificationRepository;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifChannel;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifStatus;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.notification.NotificationNotFoundException;
import com.aivle13.fin_audit_ai.global.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MailService mailService;

    // 알림 벨 목록 (최신순 페이징)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);

        Page<NotificationEntity> result = notificationRepository.findByUser_IdOrderBySentAtDesc(userId, pageable);

        List<NotificationResponse> content = result.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        return PageResponse.of(result, content);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        NotificationEntity notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);

        notification.markRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void clearAll(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    // 감사 완료 처리 시점(AuditProgressService)에서 호출되는 실제 알림 생성 지점.
    // 법령 개정 알림은 이를 발생시키는 크롤러·배치가 아직 없어 생성 지점이 없다.
    @Transactional
    public void notifyAuditComplete(AuditEntity audit) {
        UserEntity user = audit.getUser();

        if (!user.isAuditCompleteAlertEnabled()) {
            return;
        }

        NotificationEntity notification = NotificationEntity.ofAuditComplete(
                user, audit, NotifChannel.IN_APP, NotifStatus.SENT, LocalDateTime.now());

        notificationRepository.save(notification);
    }

    // 감사 실패 처리 시점(AuditProgressService.markFailed(auditId, generation))에서 호출되는
    // 알림 생성 지점. 서버 재시작 복구용 markFailed(auditId)에서는 호출하지 않는다 — 감사
    // 자체의 문제가 아니라 배포/재기동 사정으로 걸린 것들까지 알림이 나가면 안 되기 때문.
    @Transactional
    public void notifyAuditFailed(AuditEntity audit) {
        UserEntity user = audit.getUser();

        if (!user.isAuditFailAlertEnabled()) {
            return;
        }

        NotificationEntity notification = NotificationEntity.ofAuditFailed(
                user, audit, NotifChannel.IN_APP, NotifStatus.SENT, LocalDateTime.now());

        notificationRepository.save(notification);
    }

    // 감사 완료 처리 시점(AuditProgressService)에서, 판정 결과가 '주의(WARNING)' 이상
    // (WARNING 또는 NON_COMPLIANT)일 때만 호출되는 재감사 권고 알림 생성 지점.
    @Transactional
    public void notifyReauditRecommend(AuditEntity audit) {
        UserEntity user = audit.getUser();

        if (!user.isReauditAlertEnabled()) {
            return;
        }

        NotificationEntity notification = NotificationEntity.ofReauditRecommend(
                user, audit, NotifChannel.IN_APP, NotifStatus.SENT, LocalDateTime.now());

        notificationRepository.save(notification);
    }

    // 법령 개정 감지 시점(LawRevisionDetectionService)에서 호출되는 이메일 발송 지점.
    // 한 배치에서 여러 조문이 개정될 수 있어 사용자당 메일은 한 통으로 묶어 보내되, 알림 벨
    // (인앱) 기록은 조문 단위로 남겨야 목록에서 각 개정을 구분할 수 있어 revision마다
    // NotificationEntity를 저장한다 — 발송 상태(SENT/FAILED)는 메일 한 통 기준으로 동일하게 기록된다.
    // SMTP 발송을 DB 트랜잭션 밖에서 수행해, 발송 지연이 커넥션을 오래 잡아두거나
    // 트랜잭션 재시도 시 메일이 중복 발송되는 것을 막는다. 클래스 레벨의
    // readOnly=true를 그대로 물려받으면 save()가 read-only 트랜잭션(MANUAL flush) 안에서
    // 실행돼 커밋 시 flush가 안 되고 조용히 유실될 수 있어, NOT_SUPPORTED로 트랜잭션 자체를
    // 끊는다. save()는 Spring Data JPA가 호출마다 자체적으로 트랜잭션을 열어 처리한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void notifyLawRevisions(UserEntity user, List<LawRevisionEntity> revisions) {
        if (!user.isLawEmailEnabled() || revisions.isEmpty()) {
            return;
        }

        NotifStatus status;
        try {
            List<String> titles = revisions.stream().map(LawRevisionEntity::getTitle).toList();
            mailService.sendLawRevisionMail(user.getEmail(), titles);
            status = NotifStatus.SENT;
        } catch (BusinessException exception) {
            status = NotifStatus.FAILED;
        }

        for (LawRevisionEntity revision : revisions) {
            NotificationEntity notification = NotificationEntity.ofLawRevision(
                    user, revision, NotifChannel.EMAIL, status, LocalDateTime.now());

            notificationRepository.save(notification);
        }
    }
}