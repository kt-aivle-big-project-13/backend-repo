package com.aivle13.fin_audit_ai.domain.notification.controller;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.core.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.notification.entity.NotificationEntity;
import com.aivle13.fin_audit_ai.domain.notification.repository.NotificationRepository;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifChannel;
import com.aivle13.fin_audit_ai.domain.notification.type.NotifStatus;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 알림 컨트롤러 통합 테스트.
 *
 * <p>알림은 사용자별로만 보여야 하고 읽음 처리가 남의 알림에 닿으면 안 된다. 페이징
 * 파라미터 검증도 컨트롤러에만 있어 서비스 테스트로는 확인되지 않는다.
 */
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiModelRepository aiModelRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Long userId;
    private Long otherUserId;
    private Long notificationId;
    private Long otherUsersNotificationId;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.create(
                "홍길동", "테스트기관", "notification-test@example.com", "hash", UserRole.AUDITOR
        ));
        userId = user.getId();

        UserEntity other = userRepository.save(UserEntity.create(
                "임꺽정", "타기관", "notification-other@example.com", "hash", UserRole.AUDITOR
        ));
        otherUserId = other.getId();

        AuditEntity audit = createAudit(user, "notification-test");
        AuditEntity otherAudit = createAudit(other, "notification-other");

        // 안읽음 2건 + 읽음 1건. 미읽음 수와 전체 읽음 처리를 함께 볼 수 있게 둔다.
        notificationId = saveNotification(user, audit).getId();
        saveNotification(user, audit);

        NotificationEntity read = saveNotification(user, audit);
        read.markRead();
        notificationRepository.save(read);

        otherUsersNotificationId = saveNotification(other, otherAudit).getId();
    }

    @Test
    @DisplayName("미읽음 수는 내 알림만 센다")
    void unreadCount() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(asOtherUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("목록은 내 알림만 보인다")
    void list() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("페이지·크기가 범위를 벗어나면 400을 반환한다")
    void listWithInvalidPaging() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/notifications")
                        .param("size", "101")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("단건 읽음 처리 후 미읽음 수가 줄어든다")
    void markAsRead() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
                        .with(authentication(asUser())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(asUser())))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("다른 사용자의 알림은 읽음 처리할 수 없다")
    void markAnotherUsersNotificationAsRead() throws Exception {
        mockMvc.perform(patch(
                        "/api/v1/notifications/{id}/read", otherUsersNotificationId)
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());

        // 남의 미읽음 수는 그대로여야 한다.
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(asOtherUser())))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("전체 읽음 처리는 내 알림만 대상으로 한다")
    void markAllAsRead() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .with(authentication(asUser())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(asUser())))
                .andExpect(jsonPath("$.unreadCount").value(0));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(asOtherUser())))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("인증 없이 접근하면 401을 반환한다")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    private AuditEntity createAudit(UserEntity user, String keyPrefix) {
        AiModelEntity model = aiModelRepository.save(AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST,
                ModelDomain.CREDIT_SCORING, "models/%s.json".formatted(keyPrefix), "1.0.0"
        ));

        DatasetEntity dataset = datasetRepository.save(DatasetEntity.create(
                model, DataSource.CUSTOMER,
                "datasets/%s.csv".formatted(keyPrefix), 100, "age,gender,income"
        ));

        return auditRepository.save(AuditEntity.create(
                model, dataset, user, "1차 정기감사", "gender",
                null, ThresholdMethod.MANUAL, null, new BigDecimal("0.5000"), null
        ));
    }

    private NotificationEntity saveNotification(UserEntity user, AuditEntity audit) {
        return notificationRepository.save(NotificationEntity.ofReauditRecommend(
                user, audit, NotifChannel.EMAIL, NotifStatus.SENT, LocalDateTime.now()
        ));
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Authentication asOtherUser() {
        return new UsernamePasswordAuthenticationToken(otherUserId, null, List.of());
    }
}
