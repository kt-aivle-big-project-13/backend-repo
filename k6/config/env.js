export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const TEST_USER = {
    email: __ENV.TEST_EMAIL || 'loadtest@example.com',
    password: __ENV.TEST_PASSWORD || 'LoadTest123!',
};

// 대상 리소스 ID는 실행 시 --env로 주입 (테스트 데이터에 맞게)
export const AUDIT_ID = __ENV.AUDIT_ID || '1';
export const MODEL_ID = __ENV.MODEL_ID || '1';

// 챗봇 시나리오용. 대화는 미리 만들어 두고 그 ID를 넘긴다 — 시나리오가 매 반복마다
// 대화를 새로 만들면 측정하려는 질문 경로가 아니라 대화 생성까지 같이 재게 된다.
export const CONVERSATION_ID = __ENV.CONVERSATION_ID || '1';

export const CHAT_QUESTION =
    __ENV.CHAT_QUESTION || '이번 감사에서 공정성 지표가 기준을 넘은 항목이 무엇인가요?';

// 리포트 생성 시나리오용. 한 번 실행할 때 만들 산출물 포맷과 VU당 반복 횟수.
// LLM 호출과 S3 업로드가 실제로 일어나므로 반복 횟수로 비용 상한을 둔다.
export const REPORT_FORMATS = (__ENV.REPORT_FORMATS || 'PDF').split(',');

export const REPORT_VUS = Number(__ENV.REPORT_VUS || 3);
export const REPORT_ITERATIONS = Number(__ENV.REPORT_ITERATIONS || 3);

// 챗봇 질문 시나리오용. 기본 쿼터가 사용자·감사당 하루 50건이라
// 기본값도 그 안에 들어오도록 잡는다.
export const CHAT_VUS = Number(__ENV.CHAT_VUS || 5);
export const CHAT_ITERATIONS = Number(__ENV.CHAT_ITERATIONS || 8);
