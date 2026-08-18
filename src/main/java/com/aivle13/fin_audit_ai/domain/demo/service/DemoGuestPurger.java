package com.aivle13.fin_audit_ai.domain.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게스트 계정과 그 계정이 만든 데이터를 지운다.
 *
 * <p>외래키에 {@code ON DELETE CASCADE} 가 걸려 있지 않아 자식부터 순서대로 지워야 한다.
 * 실제 사용자의 데이터에도 적용되는 제약이라 시연 기능을 위해 스키마 의미를 바꾸는 대신,
 * 지우는 순서를 여기에 명시적으로 둔다.
 *
 * <p>JPA 파생 삭제 대신 네이티브 문장을 쓰는 이유는 두 가지다. 엔티티를 전부 읽어 올
 * 필요가 없고, 삭제 순서가 한 곳에 순서대로 드러나 빠뜨린 테이블을 찾기 쉽다.
 *
 * <p>테이블을 새로 추가하면서 여기에 넣지 않으면 외래키 위반으로 정리가 실패한다.
 * 조용히 넘어가지 않고 실패하는 편이 낫다 — 남은 데이터를 모르고 지나치는 것보다 낫다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoGuestPurger {

    /**
     * 자식 → 부모 순서. 각 문장은 지울 사용자 id 목록({@code :userIds})만 파라미터로 받는다.
     */
    private static final List<String> DELETE_STATEMENTS = List.of(
            """
            DELETE FROM chat_message_citations
            WHERE message_id IN (
                SELECT m.message_id FROM chat_messages m
                JOIN chat_conversations c ON c.conversation_id = m.conversation_id
                WHERE c.user_id IN (:userIds))
            """,
            """
            DELETE FROM diagnosis_answers
            WHERE diagnosis_id IN (
                SELECT diagnosis_id FROM pre_diagnoses WHERE user_id IN (:userIds))
            """,
            """
            DELETE FROM chat_messages
            WHERE conversation_id IN (
                SELECT conversation_id FROM chat_conversations WHERE user_id IN (:userIds))
            """,
            "DELETE FROM chat_conversations WHERE user_id IN (:userIds)",

            // 여기부터는 감사에 딸린 것들이다.
            "DELETE FROM audit_law_mappings WHERE audit_id IN (SELECT audit_id FROM audits WHERE user_id IN (:userIds))",
            "DELETE FROM fairness_group_stats WHERE audit_id IN (SELECT audit_id FROM audits WHERE user_id IN (:userIds))",
            "DELETE FROM fairness_results WHERE audit_id IN (SELECT audit_id FROM audits WHERE user_id IN (:userIds))",
            "DELETE FROM report_narratives WHERE audit_id IN (SELECT audit_id FROM audits WHERE user_id IN (:userIds))",
            "DELETE FROM reports WHERE audit_id IN (SELECT audit_id FROM audits WHERE user_id IN (:userIds))",
            "DELETE FROM self_check_answers WHERE audit_id IN (SELECT audit_id FROM audits WHERE user_id IN (:userIds))",
            "DELETE FROM shap_feature_importances WHERE audit_id IN (SELECT audit_id FROM audits WHERE user_id IN (:userIds))",
            "DELETE FROM xai_results WHERE audit_id IN (SELECT audit_id FROM audits WHERE user_id IN (:userIds))",

            // 알림은 감사와 사용자 양쪽을 참조하므로 감사보다 먼저 지운다.
            "DELETE FROM notifications WHERE user_id IN (:userIds)",

            "DELETE FROM audits WHERE user_id IN (:userIds)",

            // 모델에 딸린 것들.
            "DELETE FROM objections WHERE model_id IN (SELECT model_id FROM ai_models WHERE user_id IN (:userIds))",
            "DELETE FROM datasets WHERE model_id IN (SELECT model_id FROM ai_models WHERE user_id IN (:userIds))",
            "DELETE FROM pre_diagnoses WHERE user_id IN (:userIds)",
            "DELETE FROM ai_models WHERE user_id IN (:userIds)",

            "DELETE FROM users WHERE user_id IN (:userIds)"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public void purge(List<Long> guestUserIds) {
        if (guestUserIds.isEmpty()) {
            return;
        }

        MapSqlParameterSource parameters =
                new MapSqlParameterSource("userIds", guestUserIds);

        for (String statement : DELETE_STATEMENTS) {
            jdbcTemplate.update(statement, parameters);
        }

        log.info("시연용 게스트 계정 {}건과 딸린 데이터를 정리했습니다.", guestUserIds.size());
    }
}
