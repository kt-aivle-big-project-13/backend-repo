package com.aivle13.fin_audit_ai.domain.demo.service;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시연용 이의제기.
 *
 * <p>세 건을 서로 다른 상태로 둔다. 목록에서 초안·승인·발송이 한눈에 보여야 이 화면이
 * 무엇을 하는 곳인지 설명 없이 전달된다.
 *
 * <ul>
 *   <li>초안 — 대응문서가 아직 없다. 시연에서 생성 버튼을 눌러 볼 대상이다.</li>
 *   <li>승인 — 대응문서를 만들어 승인까지 마쳤고 발송 전이다.</li>
 *   <li>발송 — 고객에게 안내까지 나간 상태다.</li>
 * </ul>
 *
 * <p>이의제기 번호는 모델 단위로만 유일하면 되고(V3 마이그레이션) 게스트마다 모델이
 * 따로이므로, 고정 번호를 써도 서로 부딪히지 않는다.
 */
final class DemoObjectionFixture {

    private static final String DEMO_RECIPIENT_EMAIL = "customer@demo.invalid";

    private DemoObjectionFixture() {
    }

    static List<ObjectionEntity> objections(AiModelEntity model, UserEntity guest) {
        LocalDateTime now = LocalDateTime.now();

        return List.of(
                draft(model, now),
                approved(model, guest, now),
                delivered(model, guest, now)
        );
    }

    private static ObjectionEntity draft(AiModelEntity model, LocalDateTime now) {
        return ObjectionEntity.create(
                "2026-0001",
                model,
                "김민준",
                "신용점수 미달",
                "신용점수 산정 근거에 대한 이의제기",
                """
                최근 2년간 연체 이력이 없고 소득도 늘었는데 한도가 오히려 줄었습니다.
                어떤 항목이 점수를 낮췄는지 구체적으로 알려주시기 바랍니다.
                """,
                "EXT_SOURCE_2, AMT_CREDIT, DAYS_EMPLOYED",
                "연체 이력은 없으나 외부 신용지표가 기준선을 밑돌아 자동 거절된 건임. 재심사 여지 검토 필요.",
                now.minusDays(2)
        );
    }

    private static ObjectionEntity approved(
            AiModelEntity model,
            UserEntity guest,
            LocalDateTime now
    ) {
        ObjectionEntity objection = ObjectionEntity.create(
                "2026-0002",
                model,
                "이서연",
                "소득 대비 부채비율 초과",
                "부채비율 산정 시 상환 완료 대출 반영 요청",
                """
                지난달 상환을 마친 대출이 아직 부채로 잡혀 있는 것 같습니다.
                최신 상환 내역을 반영해 다시 심사해 주시기 바랍니다.
                """,
                "AMT_ANNUITY, AMT_CREDIT, AMT_INCOME_TOTAL",
                "상환 완료 건이 심사 시점 데이터에 반영되지 않았음을 확인함. 재심사 대상으로 판단함.",
                now.minusDays(5)
        );

        objection.recordDraft(
                """
                고객님께서 제기하신 이의를 검토한 결과, 심사 시점의 부채 정보에 최근 상환 내역이
                반영되지 않았음을 확인하였습니다. 갱신된 자료를 기준으로 재심사를 진행할 예정이며,
                결과는 영업일 기준 5일 이내에 다시 안내드리겠습니다.
                """
        );

        objection.approve(guest, ObjectionDecision.REEXAMINATION, now.minusDays(3));

        return objection;
    }

    private static ObjectionEntity delivered(
            AiModelEntity model,
            UserEntity guest,
            LocalDateTime now
    ) {
        ObjectionEntity objection = ObjectionEntity.create(
                "2026-0003",
                model,
                "박지호",
                "재직기간 부족",
                "재직기간 산정 기준에 대한 이의제기",
                """
                동일 업종에서 계속 근무했는데 이직 때문에 재직기간이 초기화된 것으로 보입니다.
                경력 전체를 반영해 주실 수 있는지 확인 부탁드립니다.
                """,
                "DAYS_EMPLOYED, EXT_SOURCE_3",
                "현 직장 재직기간이 심사 기준에 미달함. 산정 기준은 현 직장 기준으로 정해져 있어 원심 유지가 타당함.",
                now.minusDays(12)
        );

        objection.recordDraft(
                """
                고객님께서 제기하신 이의를 검토하였습니다. 재직기간은 현재 직장을 기준으로 산정하도록
                내부 심사 기준에 정해져 있어, 이번 건은 기존 심사 결과를 유지하게 되었습니다.
                재직기간 요건을 충족하시는 시점에 다시 신청해 주시면 재심사가 가능합니다.
                """
        );

        objection.approve(guest, ObjectionDecision.REJECT_MAINTAIN, now.minusDays(10));
        objection.deliver(now.minusDays(9), DEMO_RECIPIENT_EMAIL);

        return objection;
    }
}
