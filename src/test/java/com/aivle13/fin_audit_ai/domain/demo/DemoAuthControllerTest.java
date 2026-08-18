package com.aivle13.fin_audit_ai.domain.demo;

import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.ShapFeatureImportanceRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 시연용 게스트 발급 통합 테스트.
 *
 * <p>인증 없이 열려 있는 경로이므로, 실제로 토큰이 나오고 둘러볼 데이터까지 함께 만들어지는지
 * 끝에서 끝까지 확인한다.
 */
@Transactional
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.demo.enabled=true",
        "app.demo.model-s3-key=demo/credit_model.json",
        "app.demo.dataset-s3-key=demo/audit_dataset.csv"
})
class DemoAuthControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiModelRepository aiModelRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private FairnessResultRepository fairnessResultRepository;

    @Autowired
    private XaiResultRepository xaiResultRepository;

    @Autowired
    private ShapFeatureImportanceRepository shapFeatureImportanceRepository;

    @Test
    @DisplayName("인증 없이 게스트 계정을 발급받는다")
    void issuesGuestWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    @DisplayName("게스트마다 다른 계정이 발급된다")
    void issuesDistinctGuestPerRequest() throws Exception {
        Long first = issueGuestId();
        Long second = issueGuestId();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("게스트에게 감사에 바로 쓸 수 있는 모델과 데이터셋이 생긴다")
    void provisionsModelAndDataset() throws Exception {
        Long guestId = issueGuestId();

        var models = aiModelRepository.findAll().stream()
                .filter(model -> model.getUser().getId().equals(guestId))
                .toList();

        assertThat(models).hasSize(1);

        var datasets = datasetRepository.findAll().stream()
                .filter(dataset -> dataset.getModel().getId().equals(models.get(0).getId()))
                .toList();

        assertThat(datasets).hasSize(1);
        // 민감정보가 선택돼 있지 않으면 감사 시작이 400 으로 막힌다.
        assertThat(datasets.get(0).getSensitiveAttributes()).isNotBlank();
    }

    @Test
    @DisplayName("게스트에게 완료된 감사 결과가 함께 생긴다")
    void provisionsCompletedAudit() throws Exception {
        Long guestId = issueGuestId();

        var audits = auditRepository.findAll().stream()
                .filter(audit -> audit.getUser().getId().equals(guestId))
                .toList();

        assertThat(audits).hasSize(1);

        var audit = audits.get(0);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.WARNING);
        assertThat(audit.getCompletedAt()).isNotNull();

        Long auditId = audit.getId();

        assertThat(fairnessResultRepository.findAllByAudit_Id(auditId)).isNotEmpty();
        assertThat(xaiResultRepository.findAllByAudit_Id(auditId)).isNotEmpty();
        assertThat(shapFeatureImportanceRepository.findAllByAudit_IdOrderByRankAsc(auditId)).isNotEmpty();
    }

    @Test
    @DisplayName("게스트 계정은 이메일로 실제 사용자와 구분된다")
    void guestEmailIsDistinguishable() throws Exception {
        Long guestId = issueGuestId();

        String email = userRepository.findById(guestId).orElseThrow().getEmail();

        assertThat(email).startsWith("guest-").endsWith("@demo.invalid");
    }

    private Long issueGuestId() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);

        return json.get("userId").asLong();
    }
}
