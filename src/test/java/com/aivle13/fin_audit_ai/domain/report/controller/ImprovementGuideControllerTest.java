package com.aivle13.fin_audit_ai.domain.report.controller;

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
import com.aivle13.fin_audit_ai.global.ai.client.report.ImprovementGuideClient;
import com.aivle13.fin_audit_ai.global.ai.dto.report.request.ImprovementGuideRequest;
import com.aivle13.fin_audit_ai.global.ai.dto.report.response.ImprovementGuideResponse;
import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ImprovementGuide 컨트롤러 통합 테스트.
 *
 * <p>AI 서버 호출과 S3 만 대체하고 컨트롤러·서비스·영속화·DB 는 실제로 태운다. 서비스
 * 단위 테스트로는 확인되지 않는 것들(포맷 파라미터 기본값, 다운로드 헤더, 권한)을 본다.
 */
@AutoConfigureMockMvc
@Transactional
class ImprovementGuideControllerTest extends IntegrationTestSupport {

    private static final String HTML_KEY = "improvement-guides/1/run/report.html";
    private static final String PDF_KEY = "improvement-guides/1/run/report.pdf";
    private static final String WORD_KEY = "improvement-guides/1/run/report.docx";

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

    @MockitoBean
    private ImprovementGuideClient improvementGuideClient;

    @MockitoBean
    private FileStorageService fileStorageService;

    private Long userId;
    private Long otherUserId;
    private Long auditId;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.create(
                "테스트기관", "홍길동", "improvement-report-test@example.com", "hash", UserRole.AUDITOR
        ));
        userId = user.getId();

        otherUserId = userRepository.save(UserEntity.create(
                "타기관", "임꺽정", "improvement-report-other@example.com", "hash", UserRole.AUDITOR
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

        when(improvementGuideClient.generate(any(ImprovementGuideRequest.class)))
                .thenReturn(new ImprovementGuideResponse(
                        auditId, HTML_KEY, PDF_KEY, WORD_KEY,
                        "html", 3, 1, 1, "2026-08-05T10:00:00Z", List.of()
                ));
    }

    @Test
    @DisplayName("리포트를 생성하면 201과 HTML 리포트 ID를 반환한다")
    void generate() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/reports/improvement", auditId)
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").isNumber());
    }

    @Test
    @DisplayName("포맷을 지정하지 않으면 HTML 리포트를 반환한다")
    void getLatestDefaultsToHtml() throws Exception {
        generateReport();

        mockMvc.perform(get("/api/v1/audits/{auditId}/reports/improvement", auditId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("HTML"))
                .andExpect(jsonPath("$.reportType").value("IMPROVEMENT_GUIDE"));
    }

    @Test
    @DisplayName("포맷별로 서로 다른 리포트를 조회한다")
    void getLatestByFormat() throws Exception {
        generateReport();

        Long htmlId = reportIdOf("HTML");
        Long pdfId = reportIdOf("PDF");
        Long wordId = reportIdOf("WORD");

        // 한 번 생성하면 세 포맷이 모두 저장되므로 서로 다른 리포트여야 한다.
        org.assertj.core.api.Assertions.assertThat(List.of(htmlId, pdfId, wordId))
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("PDF 다운로드는 application/pdf와 pdf 파일명으로 응답한다")
    void downloadPdf() throws Exception {
        generateReport();
        givenStoredFile("%PDF-1.4 test".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get(
                        "/api/v1/audits/{auditId}/reports/improvement/{reportId}/download",
                        auditId, reportIdOf("PDF"))
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString(
                                "improvement-guide-%d.pdf".formatted(auditId))
                ));
    }

    @Test
    @DisplayName("Word 다운로드는 docx 확장자와 Content-Type으로 응답한다")
    void downloadWord() throws Exception {
        generateReport();
        givenStoredFile(new byte[]{0x50, 0x4b, 0x03, 0x04});

        mockMvc.perform(get(
                        "/api/v1/audits/{auditId}/reports/improvement/{reportId}/download",
                        auditId, reportIdOf("WORD"))
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Type",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ))
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString(
                                "improvement-guide-%d.docx".formatted(auditId))
                ));
    }

    @Test
    @DisplayName("생성된 리포트가 없으면 404를 반환한다")
    void getLatestWithoutReport() throws Exception {
        mockMvc.perform(get("/api/v1/audits/{auditId}/reports/improvement", auditId)
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 사용자의 감사로는 생성할 수 없다")
    void generateForAnotherUsersAudit() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/reports/improvement", auditId)
                        .with(authentication(asOtherUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 없이 접근하면 401을 반환한다")
    void unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/reports/improvement", auditId))
                .andExpect(status().isUnauthorized());
    }

    private void generateReport() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/reports/improvement", auditId)
                        .with(authentication(asUser())))
                .andExpect(status().isCreated());
    }

    private Long reportIdOf(String format) throws Exception {
        String body = mockMvc.perform(
                        get("/api/v1/audits/{auditId}/reports/improvement", auditId)
                                .param("format", format)
                                .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.parse(body).read("$.reportId", Long.class);
    }

    private void givenStoredFile(byte[] content) {
        when(fileStorageService.download(anyString()))
                .thenReturn(new DownloadedFile(
                        new ByteArrayInputStream(content),
                        "application/octet-stream",
                        content.length
                ));
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Authentication asOtherUser() {
        return new UsernamePasswordAuthenticationToken(otherUserId, null, List.of());
    }
}
