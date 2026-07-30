package com.aivle13.fin_audit_ai.global.lawapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * law.go.kr 법령 본문 조회 API(target=law, type=JSON) 응답.
 * 실제 발급받은 인증키로 호출해 확인한 필드명을 그대로 매핑한다. "항"은 항이 하나면 객체,
 * 여러 개면 배열로 내려오는 등 형태가 일정하지 않아 엄격한 레코드 대신 JsonNode로 받아
 * {@link LawGoKrTextFlattener}에서 재귀적으로 순회한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LawServiceResponse(
        @JsonProperty("법령") Law law
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Law(
            @JsonProperty("기본정보") BasicInfo basicInfo,
            @JsonProperty("조문") Articles articles
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BasicInfo(
            @JsonProperty("법령명_한글") String lawNameKorean
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Articles(
            @JsonProperty("조문단위") List<ArticleUnit> units
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArticleUnit(
            @JsonProperty("조문번호") String articleNo,
            @JsonProperty("조문가지번호") String articleSubNo,
            @JsonProperty("조문여부") String articleType,
            @JsonProperty("조문시행일자") String effectiveDate,
            @JsonProperty("조문변경여부") String changed,
            @JsonProperty("조문내용") String content,
            @JsonProperty("항") JsonNode paragraphs
    ) {
        // "전문"은 장/절 제목 같은 의사(疑似) 조문이라 조문번호가 실제 조항과 겹칠 수 있다.
        public boolean isActualArticle() {
            return "조문".equals(articleType);
        }
    }
}
