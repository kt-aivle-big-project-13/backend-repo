package com.aivle13.fin_audit_ai.domain.demo;

import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 시연 모드가 꺼져 있을 때의 동작.
 *
 * <p>운영에서 이 경로가 열려 있으면 누구나 계정 없이 데이터를 만들 수 있다. 꺼진 상태에서
 * 경로 자체가 없는 것처럼 보이는지 확인한다.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.demo.enabled=false")
class DemoDisabledTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("시연 모드가 꺼져 있으면 게스트를 발급하지 않는다")
    void doesNotIssueGuestWhenDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isNotFound());
    }
}
