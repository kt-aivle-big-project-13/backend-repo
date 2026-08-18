package com.aivle13.fin_audit_ai.domain.demo;

import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.ShapFeatureImportanceRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.objection.repository.ObjectionRepository;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionStatus;
import com.aivle13.fin_audit_ai.domain.report.repository.ReportRepository;
import com.aivle13.fin_audit_ai.domain.report.type.ReportStatus;
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
        "app.demo.dataset-s3-key=demo/audit_dataset.csv",
        "app.demo.reports.BIAS_REPORT.PDF=demo/reports/bias.pdf",
        "app.demo.reports.BIAS_REPORT.HTML=demo/reports/bias.html",
        // 비워 둔 포맷은 심지 않는다.
        "app.demo.reports.BIAS_REPORT.WORD=",
        "app.demo.reports.XAI_REPORT.PDF=demo/reports/xai.pdf"
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

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ObjectionRepository objectionRepository;

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
    @DisplayName("미리 올려 둔 리포트가 게스트 감사의 산출물로 연결된다")
    void attachesPreparedReports() throws Exception {
        Long guestId = issueGuestId();

        Long auditId = auditRepository.findAll().stream()
                .filter(audit -> audit.getUser().getId().equals(guestId))
                .findFirst()
                .orElseThrow()
                .getId();

        var reports = reportRepository.findAll().stream()
                .filter(report -> report.getAudit().getId().equals(auditId))
                .toList();

        // 지정한 3건만 연결되고, 비워 둔 WORD 는 심지 않는다.
        assertThat(reports).hasSize(3);

        assertThat(reports)
                .allSatisfy(report ->
                        assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED));

        assertThat(reports)
                .extracting(report -> report.getReportType() + "." + report.getFormat())
                .containsExactlyInAnyOrder(
                        "BIAS_REPORT.PDF",
                        "BIAS_REPORT.HTML",
                        "XAI_REPORT.PDF"
                );

        // 파일은 공용 데모 객체를 그대로 가리킨다.
        assertThat(reports)
                .extracting(report -> report.getFilePath())
                .allSatisfy(path -> assertThat(path).startsWith("demo/reports/"));
    }

    @Test
    @DisplayName("게스트에게 상태가 다른 이의제기 세 건이 생긴다")
    void provisionsObjectionsInEachStatus() throws Exception {
        Long guestId = issueGuestId();

        Long modelId = aiModelRepository.findAll().stream()
                .filter(model -> model.getUser().getId().equals(guestId))
                .findFirst()
                .orElseThrow()
                .getId();

        var objections = objectionRepository.findAll().stream()
                .filter(objection -> objection.getModel().getId().equals(modelId))
                .toList();

        assertThat(objections).hasSize(3);

        // 초안·승인·발송이 한 화면에 다 보여야 무엇을 하는 곳인지 전달된다.
        assertThat(objections)
                .extracting(objection -> objection.getStatus())
                .containsExactlyInAnyOrder(
                        ObjectionStatus.DRAFT,
                        ObjectionStatus.APPROVED,
                        ObjectionStatus.DELIVERED
                );

        // 승인·발송 건은 대응문서가 이미 있어야 생성을 기다리지 않는다.
        assertThat(objections)
                .filteredOn(objection -> objection.getStatus() != ObjectionStatus.DRAFT)
                .allSatisfy(objection ->
                        assertThat(objection.getDraftContent()).isNotBlank());
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
