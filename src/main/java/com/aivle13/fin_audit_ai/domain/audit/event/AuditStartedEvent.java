package com.aivle13.fin_audit_ai.domain.audit.event;

// generation은 이 이벤트가 몇 번째 실행(최초 시작 또는 몇 번째 재시도)을 위한 것인지
// 나타낸다. 리스너가 이 값을 그대로 콜백에 실어 보내, 뒤늦게 도착한 이전 실행의
// 응답이 재시도로 새로 시작된 실행의 상태를 덮어쓰지 않도록 막는 데 쓴다.
public record AuditStartedEvent(
        Long auditId,
        int generation
) {
}