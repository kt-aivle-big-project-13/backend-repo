package com.aivle13.fin_audit_ai.domain.chat.controller;

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
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.ai.client.chat.ChatAnswerClient;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.request.ChatAnswerRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.chat.response.ChatAnswerResponse;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 감사 질의 챗봇 컨트롤러 통합 테스트.
 *
 * <p>AI 답변 생성만 대체하고 컨트롤러·서비스·근거 조립·영속화·DB 는 실제로 태운다.
 * 서비스 단위 테스트로는 확인되지 않는 것들(요청 본문 검증, 질문·답변 저장 순서,
 * 권한, 삭제 연쇄)을 본다.
 */
@AutoConfigureMockMvc
@Transactional
class ChatControllerTest extends IntegrationTestSupport {

    private static final String QUESTION = "AGE_GROUP의 승인율 격차가 큰가요?";

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
    private EntityManager entityManager;

    @MockitoBean
    private ChatAnswerClient chatAnswerClient;

    private Long userId;
    private Long otherUserId;
    private Long auditId;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.create(
                "테스트기관", "홍길동", "chat-test@example.com", "hash", UserRole.AUDITOR
        ));
        userId = user.getId();

        otherUserId = userRepository.save(UserEntity.create(
                "타기관", "임꺽정", "chat-other@example.com", "hash", UserRole.AUDITOR
        )).getId();

        AiModelEntity model = aiModelRepository.save(AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST,
                ModelDomain.CREDIT_SCORING, "models/model-key.json", "1.0.0"
        ));

        DatasetEntity dataset = datasetRepository.save(DatasetEntity.create(
                model, DataSource.CUSTOMER, "datasets/audit.csv", 100, "CODE_GENDER,AGE_GROUP,TARGET"
        ));

        AuditEntity audit = auditRepository.save(AuditEntity.create(
                model, dataset, user, "1차 정기감사", "CODE_GENDER,AGE_GROUP",
                null, ThresholdMethod.MANUAL, null, new BigDecimal("0.5000"), null
        ));
        auditId = audit.getId();

        when(chatAnswerClient.generate(any(ChatAnswerRequest.class)))
                .thenReturn(new ChatAnswerResponse(
                        auditId,
                        "AGE_GROUP 의 Equal Opportunity Difference 는 0.1123 입니다.",
                        List.of(new ChatAnswerResponse.Citation(
                                "AUDIT_METRIC",
                                "EQUAL_OPPORTUNITY / AGE_GROUP",
                                "0.1123",
                                null,
                                null,
                                false
                        )),
                        "GROUNDED",
                        "2026-08-05T10:00:00Z"
                ));
    }

    @Test
    @DisplayName("대화를 만들면 201과 대화 정보를 반환한다")
    void createConversation() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/conversations", auditId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"편향 지표 문의\"}")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditId").value(auditId))
                .andExpect(jsonPath("$.title").value("편향 지표 문의"));
    }

    @Test
    @DisplayName("제목 없이 만들면 기본 제목이 붙는다")
    void createConversationWithoutTitle() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/conversations", auditId)
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").isNotEmpty());
    }

    @Test
    @DisplayName("질문하면 답변과 인용, 근거 충실도를 함께 반환한다")
    void ask() throws Exception {
        Long conversationId = createConversation("질의");

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questionBody(QUESTION))
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.groundingStatus").value("GROUNDED"))
                .andExpect(jsonPath("$.citations[0].type").value("AUDIT_METRIC"))
                .andExpect(jsonPath("$.citations[0].reference")
                        .value("EQUAL_OPPORTUNITY / AGE_GROUP"));
    }

    @Test
    @DisplayName("대화 이력은 질문과 답변이 시간순으로 쌓인다")
    void getMessages() throws Exception {
        Long conversationId = createConversation("질의");
        ask(conversationId, QUESTION);

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].content").value(QUESTION))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"))
                // 질문에는 근거 판정이 없다.
                .andExpect(jsonPath("$[0].groundingStatus").doesNotExist());
    }

    @Test
    @DisplayName("빈 질문은 400을 반환한다")
    void askWithBlankQuestion() throws Exception {
        Long conversationId = createConversation("질의");

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questionBody("   "))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("질문이 2000자를 넘으면 400을 반환한다")
    void askWithTooLongQuestion() throws Exception {
        Long conversationId = createConversation("질의");

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questionBody("가".repeat(2001)))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("대화 목록은 내 대화만 보인다")
    void getConversations() throws Exception {
        createConversation("첫 번째");
        createConversation("두 번째");

        mockMvc.perform(get("/api/v1/audits/{auditId}/conversations", auditId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/audits/{auditId}/conversations", auditId)
                        .with(authentication(asOtherUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 사용자의 대화에는 질문할 수 없다")
    void askOnAnotherUsersConversation() throws Exception {
        Long conversationId = createConversation("질의");

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questionBody(QUESTION))
                        .with(authentication(asOtherUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("대화를 지우면 그 안의 질문·답변도 함께 지워진다")
    void deleteConversation() throws Exception {
        Long conversationId = createConversation("질의");
        ask(conversationId, QUESTION);

        // 운영에서는 요청마다 트랜잭션이 나뉘어, 삭제 시 메시지를 DB 에서 읽어 연쇄 삭제한다.
        // 테스트는 한 트랜잭션으로 묶이므로 비우지 않으면 저장된 메시지가 컨텍스트에 남아
        // 실제와 다른 상태가 된다.
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/conversations/{id}", conversationId)
                        .with(authentication(asUser())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 없이 접근하면 401을 반환한다")
    void unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/conversations", auditId))
                .andExpect(status().isUnauthorized());
    }

    private Long createConversation(String title) throws Exception {
        String body = mockMvc.perform(
                        post("/api/v1/audits/{auditId}/conversations", auditId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"%s\"}".formatted(title))
                                .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.parse(body).read("$.conversationId", Long.class);
    }

    private void ask(Long conversationId, String question) throws Exception {
        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questionBody(question))
                        .with(authentication(asUser())))
                .andExpect(status().isCreated());
    }

    private String questionBody(String question) {
        return "{\"question\":\"%s\"}".formatted(question);
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Authentication asOtherUser() {
        return new UsernamePasswordAuthenticationToken(otherUserId, null, List.of());
    }
}
