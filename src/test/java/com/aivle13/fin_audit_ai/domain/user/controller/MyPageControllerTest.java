package com.aivle13.fin_audit_ai.domain.user.controller;

import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 마이페이지 컨트롤러 통합 테스트.
 *
 * <p>비밀번호 확인·변경·탈퇴는 실제 인코더와 DB 저장까지 함께 맞아야 동작한다. 응답이
 * 비밀번호 해시를 그대로 흘리지 않는지도 여기서 본다.
 */
@AutoConfigureMockMvc
@Transactional
class MyPageControllerTest extends IntegrationTestSupport {

    private static final String RAW_PASSWORD = "Test1234!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userId;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.create(
                "홍길동", "테스트기관", "mypage-test@example.com",
                passwordEncoder.encode(RAW_PASSWORD), UserRole.AUDITOR
        ));
        userId = user.getId();
    }

    @Test
    @DisplayName("내 프로필을 조회하면 비밀번호는 마스킹되어 나온다")
    void getMyProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mypage-test@example.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.institution").value("테스트기관"))
                .andExpect(jsonPath("$.password").value("********"));
    }

    @Test
    @DisplayName("이름을 수정하면 저장된 값이 바뀐다")
    void updateMyProfile() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  김감사  \"}")
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김감사"));
    }

    @Test
    @DisplayName("이름이 비어 있으면 400을 반환한다")
    void updateMyProfileWithBlankName() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("알림 설정을 저장하면 응답과 저장값이 함께 바뀐다")
    void updateNotificationPreferences() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lawEmailEnabled": false,
                                  "reauditAlertEnabled": false,
                                  "auditCompleteAlertEnabled": true,
                                  "auditFailAlertEnabled": false
                                }
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lawEmailEnabled").value(false))
                .andExpect(jsonPath("$.auditCompleteAlertEnabled").value(true));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(asUser())))
                .andExpect(jsonPath("$.reauditAlertEnabled").value(false));
    }

    @Test
    @DisplayName("알림 설정값이 누락되면 400을 반환한다")
    void updateNotificationPreferencesWithMissingField() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lawEmailEnabled\": true}")
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("현재 비밀번호 확인은 맞을 때만 통과한다")
    void verifyCurrentPassword() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"%s\"}".formatted(RAW_PASSWORD))
                        .with(authentication(asUser())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/me/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"Wrong1234!\"}")
                        .with(authentication(asUser())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비밀번호를 변경하면 새 비밀번호로 확인이 통과한다")
    void changeMyPassword() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Test1234!",
                                  "newPassword": "NewPass1234!",
                                  "newPasswordConfirm": "NewPass1234!"
                                }
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/me/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"NewPass1234!\"}")
                        .with(authentication(asUser())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("새 비밀번호 확인값이 다르면 400을 반환하고 비밀번호는 그대로 남는다")
    void changeMyPasswordWithMismatchedConfirm() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Test1234!",
                                  "newPassword": "NewPass1234!",
                                  "newPasswordConfirm": "OtherPass1234!"
                                }
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/users/me/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"%s\"}".formatted(RAW_PASSWORD))
                        .with(authentication(asUser())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 변경되지 않는다")
    void changeMyPasswordWithWrongCurrentPassword() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Wrong1234!",
                                  "newPassword": "NewPass1234!",
                                  "newPasswordConfirm": "NewPass1234!"
                                }
                                """)
                        .with(authentication(asUser())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("탈퇴하면 비활성 처리되고 이메일이 재사용 가능하도록 바뀐다")
    void withdraw() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"%s\"}".formatted(RAW_PASSWORD))
                        .with(authentication(asUser())))
                .andExpect(status().isOk());

        UserEntity withdrawn = userRepository.findById(userId).orElseThrow();

        assertThat(withdrawn.isActive()).isFalse();
        assertThat(withdrawn.getEmail()).isNotEqualTo("mypage-test@example.com");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 탈퇴되지 않는다")
    void withdrawWithWrongPassword() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"Wrong1234!\"}")
                        .with(authentication(asUser())))
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.findById(userId).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("인증 없이 접근하면 401을 반환한다")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    private Authentication asUser() {
        // 컨트롤러가 탈퇴 시 credentials 를 액세스 토큰으로 꺼내 세션을 폐기한다.
        return new UsernamePasswordAuthenticationToken(userId, "test-access-token", List.of());
    }
}
