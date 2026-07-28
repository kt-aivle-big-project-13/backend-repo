package com.aivle13.fin_audit_ai.global.ai.client;

import com.aivle13.fin_audit_ai.global.exception.ai.AiServerErrorException;
import com.aivle13.fin_audit_ai.global.exception.ai.AiServerTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FastApiEmbeddingClientTest {

    private static final String BASE_URL = "http://localhost:8000";
    private static final String EMBEDDING_URL =
            BASE_URL + "/internal/v1/embedding/generate";

    @Test
    void returnsEmbeddingWhenAiServerSucceeds() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiEmbeddingClient client =
                new FastApiEmbeddingClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(EMBEDDING_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "text": "인공지능 기본법 제1조 요약"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "embedding": [0.1, 0.2, 0.3],
                          "model": "text-embedding-3-small"
                        }
                        """, MediaType.APPLICATION_JSON));

        float[] embedding = client.embed("인공지능 기본법 제1조 요약");

        assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f);

        server.verify();
    }

    @Test
    void throwsEa001WhenAiServerReturnsError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiEmbeddingClient client =
                new FastApiEmbeddingClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(EMBEDDING_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.embed("텍스트"))
                .isInstanceOf(AiServerErrorException.class);

        server.verify();
    }

    @Test
    void throwsEa001WhenAiServerReturnsEmptyEmbedding() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        FastApiEmbeddingClient client =
                new FastApiEmbeddingClient(
                        builder.baseUrl(BASE_URL).build()
                );

        server.expect(requestTo(EMBEDDING_URL))
                .andRespond(withSuccess("""
                        {
                          "embedding": [],
                          "model": "text-embedding-3-small"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("텍스트"))
                .isInstanceOf(AiServerErrorException.class);

        server.verify();
    }

    @Test
    void throwsEa002WhenAiServerTimesOut() {
        RestClient timeoutRestClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory((uri, httpMethod) -> {
                    throw new SocketTimeoutException(
                            "AI server timeout"
                    );
                })
                .build();

        FastApiEmbeddingClient client =
                new FastApiEmbeddingClient(timeoutRestClient);

        assertThatThrownBy(() -> client.embed("텍스트"))
                .isInstanceOf(AiServerTimeoutException.class);
    }
}
