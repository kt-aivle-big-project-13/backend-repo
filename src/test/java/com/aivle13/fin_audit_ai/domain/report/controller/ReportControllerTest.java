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
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import com.aivle13.fin_audit_ai.global.s3.dto.DownloadedFile;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 최종 감사 보고서 컨트롤러 통합 테스트.
 *
 * <p>LLM 과 S3 만 대체하고 문서 생성(PDFBox·POI)·영속화·DB 는 실제로 태운다. 포맷을 여러 개
 * 요청해도 LLM 은 한 번만 호출해야 한다는 규칙과, 다운로드 응답 헤더·권한 분리를 본다.
 */
@AutoConfigureMockMvc
@Transactional
class ReportControllerTest extends IntegrationTestSupport {

    private static final String GENERATED_CONTENT = """
            # 최종 감사 보고서

            ## 1. 감사 개요
            신용평가 모델에 대한 정기 감사 결과입니다.

            ## 2. 주요 지표
            공정성·설명가능성 지표는 기준값 이내입니다.
            """;

    @Autowired
    private MockMvc mockMvc;

    // 응답에서 포맷별 리포트 ID만 꺼내는 용도. 컨텍스트의 직렬화 설정과 무관하다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiModelRepository aiModelRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private AuditRepository auditRepository;

    @MockitoBean
    private ReportLlmClient reportLlmClient;

    @MockitoBean
    private FileStorageService fileStorageService;

    private Long userId;
    private Long otherUserId;
    private Long auditId;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.create(
                "홍길동", "테스트기관", "final-report-test@example.com", "hash", UserRole.AUDITOR
        ));
        userId = user.getId();

        otherUserId = userRepository.save(UserEntity.create(
                "임꺽정", "타기관", "final-report-other@example.com", "hash", UserRole.AUDITOR
        )).getId();

        AiModelEntity model = aiModelRepository.save(AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST,
                ModelDomain.CREDIT_SCORING, "models/final-report-test.json", "1.0.0"
        ));

        DatasetEntity dataset = datasetRepository.save(DatasetEntity.create(
                model, DataSource.CUSTOMER, "datasets/audit.csv", 100, "CODE_GENDER,AGE_GROUP,TARGET"
        ));

        auditId = auditRepository.save(AuditEntity.create(
                model, dataset, user, "1차 정기감사", "CODE_GENDER,AGE_GROUP",
                null, ThresholdMethod.MANUAL, null, new BigDecimal("0.5000"), null
        )).getId();

        when(reportLlmClient.generate(anyString(), anyString()))
                .thenReturn(GENERATED_CONTENT);

        when(fileStorageService.store(any(byte[].class), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> new StoredFile(
                        "reports/%s".formatted(invocation.getArgument(1, String.class)),
                        invocation.getArgument(1, String.class),
                        invocation.getArgument(2, String.class),
                        ((byte[]) invocation.getArgument(0)).length
                ));
    }

    @Test
    @DisplayName("포맷을 여러 개 요청해도 LLM 본문은 한 번만 생성한다")
    void generateBothFormats() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/deliverables", auditId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formats\": [\"PDF\", \"WORD\"]}")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditId").value(auditId))
                .andExpect(jsonPath("$.reports.length()").value(2))
                .andExpect(jsonPath("$.reports[0].format").value("PDF"))
                .andExpect(jsonPath("$.reports[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.reports[0].reportType").value("FINAL_AUDIT_REPORT"));

        verify(reportLlmClient, times(1)).generate(anyString(), anyString());
        verify(fileStorageService, times(2))
                .store(any(byte[].class), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("같은 포맷을 중복 요청해도 한 번만 생성한다")
    void generateWithDuplicateFormats() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/deliverables", auditId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formats\": [\"PDF\", \"PDF\"]}")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reports.length()").value(1));
    }

    @Test
    @DisplayName("포맷이 비어 있으면 생성하지 않고 빈 목록을 반환한다")
    void generateWithoutFormats() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/deliverables", auditId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formats\": []}")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reports.length()").value(0));

        verify(reportLlmClient, never()).generate(anyString(), anyString());
    }

    @Test
    @DisplayName("다른 사용자의 감사에는 보고서를 생성할 수 없다")
    void generateForAnotherUsersAudit() throws Exception {
        mockMvc.perform(post("/api/v1/audits/{auditId}/deliverables", auditId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formats\": [\"PDF\"]}")
                        .with(authentication(asOtherUser())))
                .andExpect(status().isNotFound());

        // 소유자 검증은 LLM 호출·S3 업로드보다 먼저 끝나야 한다.
        verify(reportLlmClient, never()).generate(anyString(), anyString());
        verify(fileStorageService, never())
                .store(any(byte[].class), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("최신 보고서 조회는 포맷을 지정하지 않으면 PDF를 반환한다")
    void getLatestDefaultsToPdf() throws Exception {
        generateReports();

        mockMvc.perform(get("/api/v1/audits/{auditId}/deliverables/latest", auditId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("PDF"))
                .andExpect(jsonPath("$.reportType").value("FINAL_AUDIT_REPORT"))
                .andExpect(jsonPath("$.auditId").value(auditId));

        mockMvc.perform(get("/api/v1/audits/{auditId}/deliverables/latest", auditId)
                        .param("format", "WORD")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("WORD"));
    }

    @Test
    @DisplayName("생성된 보고서가 없으면 404를 반환한다")
    void getLatestWithoutReport() throws Exception {
        mockMvc.perform(get("/api/v1/audits/{auditId}/deliverables/latest", auditId)
                        .with(authentication(asUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PDF 다운로드는 application/pdf와 pdf 파일명으로 응답한다")
    void downloadPdf() throws Exception {
        Long reportId = generateReports().get("PDF");
        givenStoredFile("%PDF-1.4 test".getBytes(StandardCharsets.UTF_8), "application/pdf");

        mockMvc.perform(get("/api/v1/deliverables/{reportId}/download", reportId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("final-audit-report-%d.pdf".formatted(auditId))
                ));
    }

    @Test
    @DisplayName("Word 다운로드는 docx 파일명으로 응답한다")
    void downloadWord() throws Exception {
        Long reportId = generateReports().get("WORD");
        givenStoredFile(new byte[]{0x50, 0x4b, 0x03, 0x04},
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        mockMvc.perform(get("/api/v1/deliverables/{reportId}/download", reportId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("final-audit-report-%d.docx".formatted(auditId))
                ));
    }

    @Test
    @DisplayName("다른 사용자의 보고서는 없는 것과 같게 404로 응답한다")
    void downloadAnotherUsersReport() throws Exception {
        Long reportId = generateReports().get("PDF");

        mockMvc.perform(get("/api/v1/deliverables/{reportId}/download", reportId)
                        .with(authentication(asOtherUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 없이 접근하면 401을 반환한다")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audits/{auditId}/deliverables/latest", auditId))
                .andExpect(status().isUnauthorized());
    }

    // PDF·WORD 보고서를 생성하고 포맷별 리포트 ID를 돌려준다.
    private java.util.Map<String, Long> generateReports() throws Exception {
        String body = mockMvc.perform(post("/api/v1/audits/{auditId}/deliverables", auditId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formats\": [\"PDF\", \"WORD\"]}")
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        java.util.Map<String, Long> reportIds = new java.util.HashMap<>();

        for (JsonNode report : objectMapper.readTree(body).get("reports")) {
            reportIds.put(report.get("format").asText(), report.get("reportId").asLong());
        }

        return reportIds;
    }

    private void givenStoredFile(byte[] content, String contentType) {
        when(fileStorageService.download(anyString()))
                .thenReturn(new DownloadedFile(
                        new ByteArrayInputStream(content), contentType, content.length
                ));
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Authentication asOtherUser() {
        return new UsernamePasswordAuthenticationToken(otherUserId, null, List.of());
    }
}
