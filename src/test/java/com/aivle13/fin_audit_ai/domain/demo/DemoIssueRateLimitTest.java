package com.aivle13.fin_audit_ai.domain.demo;

import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게스트 발급 횟수 제한.
 *
 * <p>인증 없이 열린 경로라 반복 호출만으로 DB 를 채울 수 있다. 상한을 넘기면 429 로
 * 막히는지 본다.
 *
 * <p>다른 테스트와 카운터가 섞이지 않도록 이 클래스만 쓰는 IP 를 헤더로 지정한다.
 * ALB 뒤에서 실제로 쓰이는 경로({@code X-Forwarded-For})도 함께 확인된다.
 */
@Transactional
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.demo.enabled=true",
        "app.demo.model-s3-key=demo/credit_model.json",
        "app.demo.dataset-s3-key=demo/audit_dataset.csv",
        "app.demo.issue-limit-per-hour=2"
})
class DemoIssueRateLimitTest extends IntegrationTestSupport {

    private static final String CLIENT_IP = "203.0.113.77";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("상한을 넘긴 발급 요청은 429 로 막는다")
    void rejectsWhenIssueLimitExceeded() throws Exception {
        issue().andExpect(status().isCreated());
        issue().andExpect(status().isCreated());

        issue().andExpect(status().isTooManyRequests());
    }

    private org.springframework.test.web.servlet.ResultActions issue() throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/demo")
                        .header("X-Forwarded-For", CLIENT_IP)
        );
    }
}
