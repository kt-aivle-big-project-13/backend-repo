package com.aivle13.fin_audit_ai.domain.audit.repository.projection;

/**
 * 보고서 선생성에 필요한 감사 정보만 담는다.
 *
 * <p>엔티티를 그대로 읽으면 지연로딩된 사용자를 트랜잭션 밖에서 건드리게 되므로, 필요한
 * 두 값만 스칼라로 가져온다.
 */
public interface ReportPreGenerationTargetProjection {

    Long getUserId();

    /** 연결된 고영향 AI 사전진단. 사전진단을 건너뛴 감사면 비어 있다. */
    Long getAssessmentId();
}
