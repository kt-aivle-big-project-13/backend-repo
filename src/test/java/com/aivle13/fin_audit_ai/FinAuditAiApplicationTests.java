package com.aivle13.fin_audit_ai;

import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FinAuditAiApplicationTests extends IntegrationTestSupport {

	@Test
	@DisplayName("애플리케이션 컨텍스트가 정상적으로 로딩된다")
	void contextLoads() {
		// 본문 없음이 의도됨.
		// @SpringBootTest가 컨텍스트를 띄우는 것 자체가 검증이며,
		// 빈 생성이나 설정에 문제가 있으면 이 메서드 진입 전에 실패한다.
	}
}