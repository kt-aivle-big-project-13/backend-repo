import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, MODEL_ID } from '../config/env.js';
import { login, authHeaders } from '../lib/auth.js';

export const options = {
    stages: [
        { duration: '30s', target: 10 },   // warm-up
        { duration: '1m', target: 50 },    // ramp-up
        { duration: '1m', target: 100 },   // 목표 부하 유지
        { duration: '30s', target: 0 },    // ramp-down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 캐싱 전/후 비교 기준값 — 필요시 조정
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    const token = login();
    return { token };
}

export default function datasetListScenario(data) {
    const res = http.get(
        `${BASE_URL}/api/models/${MODEL_ID}/datasets`,
        authHeaders(data.token)
    );

    check(res, {
        '200 응답': (r) => r.status === 200,
    });

    sleep(1);
}
