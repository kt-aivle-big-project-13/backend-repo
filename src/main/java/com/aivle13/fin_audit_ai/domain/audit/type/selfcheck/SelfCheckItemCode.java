package com.aivle13.fin_audit_ai.domain.audit.type.selfcheck;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 규제 자가점검 21문항. STEP4_규제자가점검_문항정의서_v1.md 4장 기준으로, 조문 단위 7개
 * 그룹(TR·RM·UP·HO·DC·IA·SC)으로 묶여 있다 — 실무 주제가 아니라 조문 구조를 그대로 따른
 * 분류라 임의로 재분류하면 안 된다(문서 6장⑦).
 *
 * <p>TR(Transparency,투명성 확보) · RM(Risk Management,위험관리) · UP(User Protection,
 * 이용자 보호) · HO(Human Oversight,사람의 관리·감독) · DC(Documentation,문서 작성·보관) ·
 * IA(Impact Assessment,영향평가) · SC(Scope,적용범위·사업자 지위).
 *
 * <p>Java 식별자는 하이픈을 못 써서 밑줄로 쓰지만({@code TR_01}), 프론트와 주고받는 JSON
 * 값은 문서 표기 그대로 하이픈 코드({@code "TR-01"})를 쓴다({@link #code()}).
 *
 * <p>기존 5문항(NOTICE 등)은 TR-01·UP-01·HO-01·RM-01·DC-01과 질문이 완전히 동일해 이 5개로
 * 흡수됐다 — 과거 저장된 응답은 마이그레이션 러너({@code SelfCheckItemCodeMigrationRunner})로
 * 옮긴다.
 */
public enum SelfCheckItemCode {

    // TR · 투명성 확보 (제31조, 제34조①2호)
    TR_01, TR_02, TR_03, TR_04,
    // RM · 위험관리 (제34조①1호, 제34조①6호)
    RM_01, RM_02, RM_03, RM_04,
    // UP · 이용자 보호 (제34조①3호)
    UP_01, UP_02,
    // HO · 사람의 관리·감독 (제34조①4호)
    HO_01, HO_02,
    // DC · 문서 작성·보관 (제34조①5호, 제36조①3호)
    DC_01, DC_02,
    // IA · 영향평가 (제35조①) — 전부 노력의무
    IA_01, IA_02,
    // SC · 적용범위·사업자 지위 (제2조7호, 제30조③, 제32조①, 제34조③, 제36조①)
    SC_01, SC_02, SC_03, SC_04, SC_05;

    // enum 값을 전부 다뤄 컴파일러가 누락을 잡아주도록 default 없이 둔다 — 새 항목을
    // 추가하고 여기 안 채우면 컴파일이 깨진다.
    public String label() {
        return switch (this) {
            case TR_01 -> "AI 심사 사실을 고객에게 사전에 알리고 있나요?";
            case TR_02 -> "학습용데이터의 개요(출처·범위·수집 기간)를 설명 자료로 정리해 두었나요?";
            case TR_03 -> "위험관리방안·이용자 보호방안·담당자 정보를 이용자가 확인할 수 있도록 게시하고 있나요?";
            case TR_04 -> "고객 안내문·답변서를 생성형 AI로 작성하는 경우, 생성 사실을 표시하고 있나요?";
            case RM_01 -> "위험관리 규정이 수립 및 운영되고 있나요?";
            case RM_02 -> "AI 오류·이상 징후를 상시 모니터링하고, 사고 발생 시 대응하는 절차가 있나요?";
            case RM_03 -> "모델 재학습·중대 변경 시 위험을 재평가하는 주기와 기준이 정해져 있나요?";
            case RM_04 -> "고시·위원회 심의·의결 등 변경사항을 확인하고 반영하는 담당자·절차가 있나요?";
            case UP_01 -> "고객이 심사 결과에 이의를 제기할 절차가 있나요?";
            case UP_02 -> "AI로 인한 피해 발생 시 구제·보상 절차를 마련하고 있나요?";
            case HO_01 -> "AI 결정을 사람이 관리 및 감독하는 체계가 있나요?";
            case HO_02 -> "AI 심사 결과에 사람이 개입하거나 번복한 이력을 기록·보관하고 있나요?";
            case DC_01 -> "조치 내용을 문서로 작성 및 보관하고 있나요?";
            case DC_02 -> "모델 변경·법령 개정 시 문서를 최신 상태로 유지·점검하고 있나요?";
            case IA_01 -> "서비스 제공 전 사람의 기본권에 미치는 영향을 평가하고 문서화했나요?";
            case IA_02 -> "영향평가 시 고령자·장애인 등 인공지능취약계층의 특성을 반영했나요?";
            case SC_01 -> "이 AI에 대한 귀사의 지위(개발사업자/이용사업자)를 확인하고 책임 범위를 정리했나요?";
            case SC_02 -> "다른 법령에 따른 조치로 갈음하는 항목의 대응표를 문서화했나요?";
            case SC_03 -> "해외 사업자의 AI를 도입한 경우 국내대리인 지정 여부를 확인했나요?";
            case SC_04 -> "고영향 AI에 대해 사전 검·인증 취득을 검토했나요?";
            case SC_05 -> "학습에 사용된 누적 연산량이 대통령령 기준 이상인지 확인했나요?";
        };
    }

    // 법이 "~하도록 노력하여야 한다"로 쓴 노력의무 3개. '아니오'여도 위반이 아니라서
    // 개선권고가이드에서 일반 개선 권고와 분리해 별도 섹션("참고 권고")으로 다룬다.
    public boolean isEffortObligation() {
        return this == IA_01 || this == IA_02 || this == SC_04;
    }

    @JsonValue
    public String code() {
        return name().replace('_', '-');
    }

    @JsonCreator
    public static SelfCheckItemCode fromCode(String code) {
        return valueOf(code.replace('-', '_'));
    }
}
