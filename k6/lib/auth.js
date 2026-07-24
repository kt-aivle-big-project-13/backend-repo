import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TEST_USER } from '../config/env.js';

// setup()에서 1회만 호출해서 토큰을 발급받고, 각 VU가 재사용한다.
export function login() {
    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email: TEST_USER.email, password: TEST_USER.password, rememberMe: false }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    check(res, { '로그인 성공': (r) => r.status === 200 });

    return res.json('accessToken');
}

export function authHeaders(token) {
    return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };
}