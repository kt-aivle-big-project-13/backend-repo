import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, AUDIT_ID, CONVERSATION_ID } from '../config/env.js';
import { login, authHeaders } from '../lib/auth.js';

// 챗봇의 조회 경로(대화 목록·이력)만 잰다. LLM 호출도 일일 쿼터도 걸리지 않는 순수 DB 조회라
// 다른 조회 API와 같은 램프업·임계값을 그대로 쓴다. 질문 경로는 chat-ask.js 참고.
export const options = {
    stages: [
        { duration: '30s', target: 10 },   // warm-up
        { duration: '1m', target: 50 },    // ramp-up
        { duration: '1m', target: 100 },   // 목표 부하 유지
        { duration: '30s', target: 0 },    // ramp-down
    ],
    // 한 반복에서 두 API 를 부르므로 임계값도 endpoint 태그별로 나눈다. 합쳐서 재면 한쪽이
    // 기준을 넘어도 다른 쪽 결과에 희석돼 안 걸린다. 이력 응답은 인용까지 실어 목록보다
    // 무거우므로, 이력만 넘긴다면 앱이 아니라 응답 크기를 먼저 의심할 것.
    thresholds: {
        'http_req_duration{endpoint:conversation-list}': ['p(95)<500'],
        'http_req_duration{endpoint:message-history}': ['p(95)<500'],
        'http_req_failed{endpoint:conversation-list}': ['rate<0.01'],
        'http_req_failed{endpoint:message-history}': ['rate<0.01'],
    },
};

export function setup() {
    const token = login();
    return { token };
}

export default function chatHistoryScenario(data) {
    const headers = authHeaders(data.token);

    const conversations = http.get(
        `${BASE_URL}/api/v1/audits/${AUDIT_ID}/conversations`,
        { ...headers, tags: { endpoint: 'conversation-list' } }
    );

    check(conversations, {
        '대화 목록 200 응답': (r) => r.status === 200,
    });

    // 대화 이력은 인용까지 함께 실어 목록보다 응답이 크다 — 목록만 재면 실제 화면 부하를 놓친다.
    const messages = http.get(
        `${BASE_URL}/api/v1/conversations/${CONVERSATION_ID}/messages`,
        { ...headers, tags: { endpoint: 'message-history' } }
    );

    check(messages, {
        '대화 이력 200 응답': (r) => r.status === 200,
    });

    sleep(1);
}
