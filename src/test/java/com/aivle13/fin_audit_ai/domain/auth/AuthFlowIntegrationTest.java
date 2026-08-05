package com.aivle13.fin_audit_ai.domain.auth;

import com.aivle13.fin_audit_ai.domain.auth.dto.request.LoginRequest;
import com.aivle13.fin_audit_ai.domain.auth.dto.request.SignupRequest;
import com.aivle13.fin_audit_ai.domain.auth.service.RecaptchaService;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// MockMvc로 실제 회원가입, 로그인, JWT 인증 흐름 검증
@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends IntegrationTestSupport {

    private static final String EMAIL =
            "test@example.com";

    private static final String PASSWORD =
            "Password123!";

    private static final String VERIFIED_EMAIL_KEY =
            "email-verification-verified:" + EMAIL;

    // HTTP API 요청 실행
    @Autowired
    private MockMvc mockMvc;

    // 요청 객체를 JSON 문자열로 변환
    @Autowired
    private ObjectMapper objectMapper;

    // 실제 저장된 사용자 확인
    @Autowired
    private UserRepository userRepository;

    // 이메일 인증 상태 등 Redis 데이터 확인
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 실제 비밀번호 암호화 결과 확인
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 외부 reCAPTCHA 호출만 Mock 처리
    @MockitoBean
    private RecaptchaService recaptchaService;

    @BeforeEach
    void setUp() {
        // 테스트 간 사용자 데이터 초기화
        userRepository.deleteAll();

        // 테스트 간 Redis 데이터 초기화
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();

        // 회원가입에 필요한 이메일 인증 완료 상태 준비
        redisTemplate.opsForValue().set(
                VERIFIED_EMAIL_KEY,
                "true",
                Duration.ofMinutes(10)
        );
    }

    @Test
    void signupLoginAndJwtAuthenticationSuccess() throws Exception {
        // given: 회원가입 요청
        SignupRequest signupRequest = new SignupRequest(
                "에이블러",
                "에이블스쿨",
                EMAIL,
                PASSWORD,
                PASSWORD,
                true,
                true
        );

        // when & then: 회원가입 API 호출 및 성공 응답 확인
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                signupRequest
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("회원가입이 완료되었습니다."));

        // 회원가입 결과가 실제 PostgreSQL에 저장됐는지 확인
        UserEntity savedUser = userRepository
                .findByEmail(EMAIL)
                .orElseThrow();

        assertThat(savedUser.getName())
                .isEqualTo("에이블러");
        assertThat(savedUser.getInstitution())
                .isEqualTo("에이블스쿨");
        assertThat(savedUser.getEmail())
                .isEqualTo(EMAIL);

        // 평문이 아닌 BCrypt 암호화 비밀번호가 저장됐는지 확인
        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(
                PASSWORD,
                savedUser.getPasswordHash()
        )).isTrue();

        // 회원가입 완료 후 이메일 인증 상태가 삭제됐는지 확인
        assertThat(redisTemplate.hasKey(VERIFIED_EMAIL_KEY))
                .isFalse();

        // given: 로그인 요청
        LoginRequest loginRequest = new LoginRequest(
                EMAIL,
                PASSWORD,
                false,
                "recaptcha-token"
        );

        // when: 로그인 API 호출
        String loginResponse = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        loginRequest
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.userId")
                        .value(savedUser.getId()))
                .andExpect(jsonPath("$.email")
                        .value(EMAIL))
                .andExpect(jsonPath("$.name")
                        .value("에이블러"))
                .andExpect(jsonPath("$.role")
                        .value("USER"))
                .andExpect(jsonPath("$.accessToken")
                        .isNotEmpty())
                .andExpect(jsonPath("$.refreshToken")
                        .isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 로그인 응답 JSON에서 실제 Access Token 추출
        JsonNode responseJson =
                objectMapper.readTree(loginResponse);

        String accessToken =
                responseJson.get("accessToken").asText();

        assertThat(accessToken)
                .isNotBlank();

        // then: 발급받은 JWT로 인증 필요 API 호출
        mockMvc.perform(get("/api/v1/users/me")
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(savedUser.getId()))
                .andExpect(jsonPath("$.email")
                        .value(EMAIL))
                .andExpect(jsonPath("$.name")
                        .value("에이블러"))
                .andExpect(jsonPath("$.institution")
                        .value("에이블스쿨"))
                .andExpect(jsonPath("$.role")
                        .value("USER"));

        // 로그인 과정에서 reCAPTCHA 검증이 호출됐는지 확인
        verify(recaptchaService)
                .verify("recaptcha-token");
    }
}