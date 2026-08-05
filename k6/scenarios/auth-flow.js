import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TEST_USER } from '../config/env.js';

export const options = {
    scenarios: {
        auth_flow: {
            executor: 'ramping-vus',
            stages: [
                { duration: '10s', target: 5 },
                { duration: '20s', target: 5 },
                { duration: '10s', target: 10 },
                { duration: '20s', target: 10 },
                { duration: '10s', target: 0 },
            ],
            gracefulRampDown: '5s',
        },
    },

    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
        checks: ['rate>0.99'],
    },
};

export default function () {
    const loginResponse = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            email: TEST_USER.email,
            password: TEST_USER.password,
            rememberMe: false,
            recaptchaToken: 'load-test-token',
        }),
        {
            headers: {
                'Content-Type': 'application/json',
            },
            tags: {
                name: 'POST /api/v1/auth/login',
            },
        },
    );

    const loginSuccess = check(loginResponse, {
        '로그인 응답 상태는 200이다': (res) => res.status === 200,
        'accessToken이 발급된다': (res) => {
            try {
                return Boolean(res.json('accessToken'));
            } catch {
                return false;
            }
        },
    });

    if (!loginSuccess) {
        console.error(
            `로그인 실패: status=${loginResponse.status}, body=${loginResponse.body}`,
        );

        sleep(1);
        return;
    }

    const accessToken = loginResponse.json('accessToken');

    const meResponse = http.get(
        `${BASE_URL}/api/v1/users/me`,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            tags: {
                name: 'GET /api/v1/users/me',
            },
        },
    );

    check(meResponse, {
        '내 정보 조회 응답 상태는 200이다': (res) =>
            res.status === 200,

        '로그인한 이메일과 일치한다': (res) => {
            try {
                return res.json('email') === TEST_USER.email;
            } catch {
                return false;
            }
        },
    });

    sleep(1);
}