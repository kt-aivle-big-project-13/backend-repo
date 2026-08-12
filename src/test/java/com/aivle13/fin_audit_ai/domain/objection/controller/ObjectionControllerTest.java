package com.aivle13.fin_audit_ai.domain.objection.controller;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.repository.ObjectionRepository;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionStatus;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.llm.ReportLlmClient;
import com.aivle13.fin_audit_ai.global.mail.MailService;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이의제기 컨트롤러 통합 테스트.
 *
 * <p>CSV 파싱·중복 번호 검증·발송 확정처럼 서비스와 DB 제약이 함께 걸리는 경로를 본다.
 *
 * <p>이 클래스만 {@code @Transactional} 을 붙이지 않는다. 대응문서 조회·재생성이
 * {@code Propagation.NOT_SUPPORTED} 라 테스트 트랜잭션이 커밋되지 않으면 데이터를 못 보기
 * 때문이다. 대신 {@link #tearDown()} 에서 만든 데이터를 지운다.
 */
@AutoConfigureMockMvc
class ObjectionControllerTest extends IntegrationTestSupport {

    private static final String CSV_HEADER =
            "고객_이름,이의제기_번호,거절_금융기준,제목,내용,주요_판단_근거_변수,담당자_판단_근거,작성일시\n";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiModelRepository aiModelRepository;

    @Autowired
    private ObjectionRepository objectionRepository;

    @MockitoBean
    private ReportLlmClient reportLlmClient;

    @MockitoBean
    private MailService mailService;

    private Long userId;
    private Long otherUserId;
    private Long modelId;
    private Long objectionId;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.create(
                "홍길동", "테스트기관", "objection-test@example.com", "hash", UserRole.AUDITOR
        ));
        userId = user.getId();

        otherUserId = userRepository.save(UserEntity.create(
                "임꺽정", "타기관", "objection-other@example.com", "hash", UserRole.AUDITOR
        )).getId();

        AiModelEntity model = aiModelRepository.save(AiModelEntity.create(
                user, "credit-model", ModelType.XGBOOST,
                ModelDomain.CREDIT_SCORING, "models/objection-test.json", "1.0.0"
        ));
        modelId = model.getId();

        objectionId = objectionRepository.save(ObjectionEntity.create(
                "OBJ-2026-0001", model, "김철수", "신용점수 미달",
                "거절 사유 설명 요청", "거절 사유를 알고 싶습니다",
                "AMT_INCOME_TOTAL", "소득 대비 부채 비율이 높음",
                LocalDateTime.of(2026, 7, 31, 9, 54)
        )).getId();

        given(reportLlmClient.generate(anyString(), anyString()))
                .willReturn("고객님께 안내드립니다.");
    }

    @AfterEach
    void tearDown() {
        // 트랜잭션 롤백이 없어 직접 지운다. 자식(이의제기) → 모델 → 사용자 순.
        objectionRepository.deleteAll();
        aiModelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("CSV 업로드로 이의제기를 일괄 등록한다")
    void importCsv() throws Exception {
        String csv = CSV_HEADER
                + "이영희,OBJ-2026-0002,신용점수 미달,재심사 요청,재심사를 요청합니다,DAYS_EMPLOYED,근속기간 부족,2026-08-01 10:30\n"
                + "박민수,OBJ-2026-0003,담보 부족,거절 사유 문의,사유를 알려주세요,AMT_CREDIT,담보 대비 대출액 과다,2026-08-02 14:05\n";

        mockMvc.perform(multipart("/api/v1/objections/import")
                        .file(csvFile(csv))
                        .param("modelId", String.valueOf(modelId))
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importedCount").value(2))
                .andExpect(jsonPath("$.objections[0].objectionNo").exists())
                .andExpect(jsonPath("$.objections[0].status").value("WAITING"));

        assertThat(objectionRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("같은 모델에 이미 등록된 이의제기 번호가 있으면 409를 반환하고 아무것도 저장하지 않는다")
    void importCsvWithDuplicateNo() throws Exception {
        String csv = CSV_HEADER
                + "이영희,OBJ-2026-0002,신용점수 미달,재심사 요청,재심사를 요청합니다,DAYS_EMPLOYED,근속기간 부족,2026-08-01 10:30\n"
                + "박민수,OBJ-2026-0001,담보 부족,거절 사유 문의,사유를 알려주세요,AMT_CREDIT,담보 대비 대출액 과다,2026-08-02 14:05\n";

        mockMvc.perform(multipart("/api/v1/objections/import")
                        .file(csvFile(csv))
                        .param("modelId", String.valueOf(modelId))
                        .with(authentication(asUser())))
                .andExpect(status().isConflict());

        // setUp 에서 만든 1건만 남아야 한다.
        assertThat(objectionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("이의제기 번호는 모델 단위로만 고유하므로 다른 모델에는 같은 번호를 등록할 수 있다")
    void importCsvReusesObjectionNoAcrossModels() throws Exception {
        Long otherModelId = aiModelRepository.save(AiModelEntity.create(
                userRepository.findById(userId).orElseThrow(),
                "credit-model-v2", ModelType.XGBOOST,
                ModelDomain.CREDIT_SCORING, "models/objection-test-v2.json", "2.0.0"
        )).getId();

        // setUp 에서 modelId 에 이미 등록한 것과 같은 번호를 다른 모델로 올린다.
        String csv = CSV_HEADER
                + "박민수,OBJ-2026-0001,담보 부족,거절 사유 문의,사유를 알려주세요,AMT_CREDIT,담보 대비 대출액 과다,2026-08-02 14:05\n";

        mockMvc.perform(multipart("/api/v1/objections/import")
                        .file(csvFile(csv))
                        .param("modelId", String.valueOf(otherModelId))
                        .with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.objections[0].objectionNo").value("OBJ-2026-0001"));

        assertThat(objectionRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("헤더가 다르거나 작성일시 형식이 틀리면 400을 반환한다")
    void importCsvWithInvalidContent() throws Exception {
        mockMvc.perform(multipart("/api/v1/objections/import")
                        .file(csvFile("고객_이름,이의제기_번호\n이영희,OBJ-2026-0002\n"))
                        .param("modelId", String.valueOf(modelId))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());

        String badDate = CSV_HEADER
                + "이영희,OBJ-2026-0002,신용점수 미달,재심사 요청,재심사를 요청합니다,DAYS_EMPLOYED,근속기간 부족,2026/08/01\n";

        mockMvc.perform(multipart("/api/v1/objections/import")
                        .file(csvFile(badDate))
                        .param("modelId", String.valueOf(modelId))
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 사용자의 모델에는 이의제기를 등록할 수 없다")
    void importCsvToAnotherUsersModel() throws Exception {
        String csv = CSV_HEADER
                + "이영희,OBJ-2026-0002,신용점수 미달,재심사 요청,재심사를 요청합니다,DAYS_EMPLOYED,근속기간 부족,2026-08-01 10:30\n";

        mockMvc.perform(multipart("/api/v1/objections/import")
                        .file(csvFile(csv))
                        .param("modelId", String.valueOf(modelId))
                        .with(authentication(asOtherUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("목록은 내 모델의 이의제기만 보인다")
    void list() throws Exception {
        mockMvc.perform(get("/api/v1/objections")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].objectionNo").value("OBJ-2026-0001"));

        mockMvc.perform(get("/api/v1/objections")
                        .with(authentication(asOtherUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("상태·검색어 필터가 적용된다")
    void listWithFilters() throws Exception {
        mockMvc.perform(get("/api/v1/objections")
                        .param("status", "WAITING")
                        .with(authentication(asUser())))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/objections")
                        .param("status", "COMPLETED")
                        .with(authentication(asUser())))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/objections")
                        .param("keyword", "없는고객")
                        .with(authentication(asUser())))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("페이지·크기가 범위를 벗어나면 400을 반환한다")
    void listWithInvalidPaging() throws Exception {
        mockMvc.perform(get("/api/v1/objections")
                        .param("size", "101")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상세는 소유자만 조회할 수 있다")
    void getDetail() throws Exception {
        mockMvc.perform(get("/api/v1/objections/{id}", objectionId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectionNo").value("OBJ-2026-0001"))
                .andExpect(jsonPath("$.customerName").value("김철수"))
                .andExpect(jsonPath("$.status").value("WAITING"));

        mockMvc.perform(get("/api/v1/objections/{id}", objectionId)
                        .with(authentication(asOtherUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("대응문서는 요청한 처리 결과에 맞춰 안내문을 생성한다")
    void getDocument() throws Exception {
        mockMvc.perform(get("/api/v1/objections/{id}/document", objectionId)
                        .param("decision", "REEXAMINATION")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REEXAMINATION"))
                .andExpect(jsonPath("$.letterBody").value("고객님께 안내드립니다."));
    }

    @Test
    @DisplayName("안내문 재생성은 발송 전 건에서만 가능하다")
    void regenerate() throws Exception {
        mockMvc.perform(post("/api/v1/objections/{id}/document/regenerate", objectionId)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.letterBody").value("고객님께 안내드립니다."));

        dispatch().andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/objections/{id}/document/regenerate", objectionId)
                        .with(authentication(asUser())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("발송하면 메일이 나가고 상태가 COMPLETED 로 바뀐다")
    void dispatchObjection() throws Exception {
        dispatch()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.decision").value("REJECT_MAINTAIN"))
                .andExpect(jsonPath("$.recipientEmail").value("customer@example.com"));

        verify(mailService).sendObjectionResponseMail(
                "customer@example.com", "김철수", "이의제기 처리 결과 안내", "안내문 본문"
        );

        assertThat(objectionRepository.findById(objectionId))
                .get()
                .extracting(ObjectionEntity::getStatus)
                .isEqualTo(ObjectionStatus.DELIVERED);
    }

    @Test
    @DisplayName("이미 발송된 건은 다시 발송할 수 없다")
    void dispatchTwice() throws Exception {
        dispatch().andExpect(status().isOk());
        dispatch().andExpect(status().isConflict());

        // 중복 발송 요청으로 메일이 두 번 나가면 안 된다.
        verify(mailService).sendObjectionResponseMail(
                anyString(), anyString(), anyString(), anyString()
        );
    }

    @Test
    @DisplayName("이메일 형식이 틀리면 400을 반환하고 메일을 보내지 않는다")
    void dispatchWithInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/objections/{id}/dispatch", objectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "REJECT_MAINTAIN",
                                  "letterTitle": "이의제기 처리 결과 안내",
                                  "letterBody": "안내문 본문",
                                  "recipientEmail": "not-an-email"
                                }
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());

        verify(mailService, never()).sendObjectionResponseMail(
                anyString(), anyString(), anyString(), anyString()
        );
    }

    @Test
    @DisplayName("인증 없이 접근하면 401을 반환한다")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/objections"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions dispatch() throws Exception {
        return mockMvc.perform(post("/api/v1/objections/{id}/dispatch", objectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "decision": "REJECT_MAINTAIN",
                          "letterTitle": "이의제기 처리 결과 안내",
                          "letterBody": "안내문 본문",
                          "recipientEmail": "customer@example.com"
                        }
                        """)
                .with(authentication(asUser())));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file", "objections.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Authentication asOtherUser() {
        return new UsernamePasswordAuthenticationToken(otherUserId, null, List.of());
    }
}
