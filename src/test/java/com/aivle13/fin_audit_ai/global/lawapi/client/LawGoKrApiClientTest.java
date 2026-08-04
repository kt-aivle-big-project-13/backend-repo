package com.aivle13.fin_audit_ai.global.lawapi.client;

import com.aivle13.fin_audit_ai.global.exception.law.LawApiErrorException;
import com.aivle13.fin_audit_ai.global.exception.law.LawApiTimeoutException;
import com.aivle13.fin_audit_ai.global.lawapi.config.LawApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LawGoKrApiClientTest {

    private static final String BASE_URL = "http://localhost:8080";

    private LawApiProperties properties() {
        return new LawApiProperties(
                true, "testKey", URI.create(BASE_URL), Duration.ofSeconds(3), Duration.ofSeconds(10)
        );
    }

    @Test
    void filtersPseudoArticlesAndFlattensStructuredContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        LawGoKrApiClient client = new LawGoKrApiClient(builder.baseUrl(BASE_URL).build(), properties());

        server.expect(requestTo(containsString("/DRF/lawService.do")))
                .andRespond(withSuccess("""
                        {
                          "법령": {
                            "기본정보": { "법령명_한글": "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법" },
                            "조문": {
                              "조문단위": [
                                {
                                  "조문번호": "1",
                                  "조문여부": "전문",
                                  "조문시행일자": "20260122",
                                  "조문내용": "제1장 총칙"
                                },
                                {
                                  "조문번호": "1",
                                  "조문여부": "조문",
                                  "조문시행일자": "20260122",
                                  "조문변경여부": "N",
                                  "조문내용": "제1조(목적) 이 법은 목적을 규정한다."
                                },
                                {
                                  "조문번호": "2",
                                  "조문여부": "조문",
                                  "조문시행일자": "20260122",
                                  "조문변경여부": "Y",
                                  "항": {
                                    "호": [
                                      { "호번호": "1.", "호내용": "1. 인공지능이란 학습 등을 말한다." },
                                      { "호번호": "2.", "호내용": "2. 인공지능시스템이란 ...를 말한다." }
                                    ]
                                  }
                                },
                                {
                                  "조문번호": "3",
                                  "조문가지번호": "2",
                                  "조문여부": "조문",
                                  "조문시행일자": "20260721",
                                  "조문변경여부": "N",
                                  "조문내용": "제3조의2(전담기관) 내용"
                                }
                              ]
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<LawArticleRevision> revisions =
                client.fetchArticles("인공지능 발전과 신뢰 기반 조성 등에 관한 기본법");

        // "전문"(장 제목)은 걸러지고 실제 조문 3개만 남는다.
        assertThat(revisions).hasSize(3);
        assertThat(revisions).extracting(LawArticleRevision::articleNo)
                .containsExactly("제1조", "제2조", "제3조의2");

        // 항/호로 구조화된 조문은 평문으로 합쳐진다.
        assertThat(revisions.get(1).content())
                .contains("1. 인공지능이란 학습 등을 말한다.")
                .contains("2. 인공지능시스템이란 ...를 말한다.");

        // 조문가지번호가 있으면 "제N조의M" 형식으로 정규화된다.
        assertThat(revisions.get(2).effectiveDate()).isEqualTo(LocalDate.of(2026, 7, 21));

        // 조문변경여부(Y/N)가 changed로 그대로 매핑된다.
        assertThat(revisions.get(0).changed()).isFalse();
        assertThat(revisions.get(1).changed()).isTrue();
        assertThat(revisions.get(2).changed()).isFalse();

        server.verify();
    }

    @Test
    void skipsArticleWithBlankEffectiveDateAndKeepsOthers() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        LawGoKrApiClient client = new LawGoKrApiClient(builder.baseUrl(BASE_URL).build(), properties());

        server.expect(requestTo(containsString("/DRF/lawService.do")))
                .andRespond(withSuccess("""
                        {
                          "법령": {
                            "기본정보": { "법령명_한글": "인공지능 발전과 신뢰 기반 조성 등에 관한 기본법" },
                            "조문": {
                              "조문단위": [
                                {
                                  "조문번호": "1",
                                  "조문여부": "조문",
                                  "조문시행일자": "",
                                  "조문변경여부": "N",
                                  "조문내용": "제1조(목적) 이 법은 목적을 규정한다."
                                },
                                {
                                  "조문번호": "2",
                                  "조문여부": "조문",
                                  "조문시행일자": "20260122",
                                  "조문변경여부": "N",
                                  "조문내용": "제2조(정의) 내용"
                                }
                              ]
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<LawArticleRevision> revisions =
                client.fetchArticles("인공지능 발전과 신뢰 기반 조성 등에 관한 기본법");

        // 시행일자가 없는 제1조는 건너뛰고, 나머지 조문은 그대로 반환한다.
        assertThat(revisions).hasSize(1);
        assertThat(revisions.get(0).articleNo()).isEqualTo("제2조");

        server.verify();
    }

    @Test
    void throwsLawApiErrorWhenApiReturnsAuthFailurePayload() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        LawGoKrApiClient client = new LawGoKrApiClient(builder.baseUrl(BASE_URL).build(), properties());

        // law.go.kr은 인증 실패도 HTTP 200으로 {"result":..,"msg":..}만 내려준다 —
        // "법령" 키가 없는 것으로 오류를 판별해야 한다.
        server.expect(requestTo(containsString("/DRF/lawService.do")))
                .andRespond(withSuccess("""
                        {
                          "result": "사용자 정보 검증에 실패하였습니다.",
                          "msg": "OPEN API 호출 시 사용자 검증을 위하여 정확한 서버장비의 IP주소 및 도메인주소를 등록해 주세요."
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchArticles("아무 법령"))
                .isInstanceOf(LawApiErrorException.class);

        server.verify();
    }

    @Test
    void throwsLawApiErrorOnServerError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        LawGoKrApiClient client = new LawGoKrApiClient(builder.baseUrl(BASE_URL).build(), properties());

        server.expect(requestTo(containsString("/DRF/lawService.do")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchArticles("아무 법령"))
                .isInstanceOf(LawApiErrorException.class);

        server.verify();
    }

    @Test
    void throwsLawApiTimeoutWhenRequestTimesOut() {
        RestClient timeoutRestClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory((uri, httpMethod) -> {
                    throw new SocketTimeoutException("law.go.kr timeout");
                })
                .build();

        LawGoKrApiClient client = new LawGoKrApiClient(timeoutRestClient, properties());

        assertThatThrownBy(() -> client.fetchArticles("아무 법령"))
                .isInstanceOf(LawApiTimeoutException.class);
    }
}
