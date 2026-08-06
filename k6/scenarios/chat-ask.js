import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import {
    BASE_URL,
    CONVERSATION_ID,
    CHAT_QUESTION,
    CHAT_VUS,
    CHAT_ITERATIONS,
} from '../config/env.js';
import { login, authHeaders } from '../lib/auth.js';

/*
 * 챗봇 질문 경로(POST /conversations/{id}/messages) 부하 시나리오.
 *
 * 다른 시나리오와 설정이 다른 이유:
 *
 * 1. 반복 횟수를 고정한다 — 질문 한 건마다 RAG 검색 + LLM 호출이 실제로 일어난다.
 *    duration 기반으로 돌리면 호출 수가 응답 속도에 따라 달라져 비용이 예측되지 않는다.
 * 2. 동시성이 낮다 — 병목은 커넥션 수가 아니라 AI 서버 응답이다. 100 VU 를 걸면
 *    큐만 길어지고 서버 처리 능력에 대해 알 수 있는 게 없다.
 * 3. 429 를 실패로 세지 않는다 — 사용자·감사당 하루 질문 수 제한(app.chat.daily-question-limit,
 *    기본 50)이 있다. 제한에 걸리는 건 서버 장애가 아니라 설계대로 동작한 것이므로
 *    별도 지표(chat_quota_exceeded)로 따로 본다. 제한 자체를 재고 싶지 않다면
 *    실행 전에 APP_CHAT_DAILY_QUESTION_LIMIT 를 올려야 한다.
 *
 * 실행 후 요약에서 chat_quota_exceeded 가 0 이 아니면 그만큼은 LLM 을 타지 않은 요청이라,
 * chat_answer_duration(429 를 뺀 응답시간) 쪽을 봐야 한다.
 */

const quotaExceeded = new Rate('chat_quota_exceeded');
const answerDuration = new Trend('chat_answer_duration', true);

export const options = {
    scenarios: {
        ask: {
            executor: 'per-vu-iterations',
            vus: CHAT_VUS,
            iterations: CHAT_ITERATIONS,
            maxDuration: '10m',
        },
    },
    // 429 도 정상 응답으로 보고 http_req_failed 에서 제외한다.
    responseCallback: http.expectedStatuses(201, 429),
    thresholds: {
        // 로컬 실측 1.4~5.1초 기준. AI 서버 응답이 지배적이라 조회 API 기준값과 자릿수가 다르다.
        http_req_duration: ['p(95)<8000'],
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    const token = login();
    return { token };
}

export default function chatAskScenario(data) {
    const res = http.post(
        `${BASE_URL}/api/v1/conversations/${CONVERSATION_ID}/messages`,
        JSON.stringify({ question: CHAT_QUESTION }),
        authHeaders(data.token)
    );

    const isQuota = res.status === 429;
    quotaExceeded.add(isQuota);

    if (!isQuota) {
        answerDuration.add(res.timings.duration);
    }

    check(res, {
        '201 또는 429 응답': (r) => r.status === 201 || r.status === 429,
        // 답변이 비면 201을 받고도 화면에는 아무것도 안 나온다 — 상태 코드만으로는 안 걸린다.
        '답변 본문 있음': (r) => r.status !== 201 || Boolean(r.json('content')),
    });
}
