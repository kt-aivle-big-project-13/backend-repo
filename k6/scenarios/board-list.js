import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from '../config/env.js';
import { login, authHeaders } from '../lib/auth.js';

// TODO(#252): 게시판 목록 조회 부하 테스트. dataset-list.js 패턴을 따른다.
export const options = {
    stages: [
        { duration: '30s', target: 10 },   // warm-up
        { duration: '1m', target: 50 },    // ramp-up
        { duration: '1m', target: 100 },   // 목표 부하 유지
        { duration: '30s', target: 0 },    // ramp-down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    const token = login();
    return { token };
}

export default function boardListScenario(data) {
    const res = http.get(
        `${BASE_URL}/api/v1/posts?page=1&size=10`,
        authHeaders(data.token)
    );

    check(res, {
        '200 응답': (r) => r.status === 200,
    });

    sleep(1);
}
