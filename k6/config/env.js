export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const TEST_USER = {
    email: __ENV.TEST_EMAIL || 'loadtest@example.com',
    password: __ENV.TEST_PASSWORD || 'LoadTest123!',
};

// 대상 리소스 ID는 실행 시 --env로 주입 (테스트 데이터에 맞게)
export const AUDIT_ID = __ENV.AUDIT_ID || '1';
export const MODEL_ID = __ENV.MODEL_ID || '1';
