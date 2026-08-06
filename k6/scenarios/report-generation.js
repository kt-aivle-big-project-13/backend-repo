import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import {
    BASE_URL,
    AUDIT_ID,
    REPORT_FORMATS,
    REPORT_VUS,
    REPORT_ITERATIONS,
} from '../config/env.js';
import { login, authHeaders } from '../lib/auth.js';

/*
 * 최종 감사 보고서 생성(POST /audits/{auditId}/deliverables) 부하 시나리오.
 *
 * ⚠️ 이 시나리오는 부작용이 있다. 반복 한 번마다 LLM 을 호출하고, PDF/Word 를 만들고,
 *    S3(MinIO) 에 파일을 올리고, reports 행을 남긴다. 실행 전에 대상 환경과
 *    반복 횟수(REPORT_VUS × REPORT_ITERATIONS, 기본 3×3=9건)를 반드시 확인할 것.
 *
 * 다른 시나리오와 설정이 다른 이유:
 *
 * 1. duration 이 아니라 반복 횟수로 끊는다 — 생성 한 건이 로컬 실측 20~60초라
 *    시간 기반으로 돌리면 호출 수와 비용이 예측되지 않는다.
 * 2. 동시성이 낮다 — 병목은 웹 계층이 아니라 LLM 응답과 문서 변환이다. 여기서 보려는 건
 *    "동시 생성 요청이 몇 건까지 서로를 밀어내지 않는가" 이지 초당 처리량이 아니다.
 * 3. 임계값이 초 단위가 아니라 분 단위다 — 조회 API 기준(p(95)<500ms)과 자릿수가 다르다.
 *
 * 포맷을 여러 개 요청해도 LLM 본문은 한 번만 만들고 문서 변환만 포맷 수만큼 돈다
 * (ReportGenerationService.generate). PDF 단건과 PDF+WORD 를 비교하면 변환 비용만 분리해서
 * 볼 수 있다 — REPORT_FORMATS 로 바꿔가며 잰다.
 */

const generationDuration = new Trend('report_generation_duration', true);

export const options = {
    scenarios: {
        generate: {
            executor: 'per-vu-iterations',
            vus: REPORT_VUS,
            iterations: REPORT_ITERATIONS,
            maxDuration: '30m',
        },
    },
    thresholds: {
        // 로컬 단건 실측 20~60초. 동시 요청이 겹치면 늘어나므로 여유를 두고 잡는다.
        http_req_duration: ['p(95)<120000'],
        http_req_failed: ['rate<0.05'],
    },
};

export function setup() {
    const token = login();
    return { token };
}

export default function reportGenerationScenario(data) {
    const res = http.post(
        `${BASE_URL}/api/v1/audits/${AUDIT_ID}/deliverables`,
        JSON.stringify({ formats: REPORT_FORMATS }),
        {
            ...authHeaders(data.token),
            // 생성이 기본 타임아웃(60초)을 넘길 수 있어 k6 쪽 타임아웃도 함께 늘린다.
            timeout: '180s',
        }
    );

    generationDuration.add(res.timings.duration);

    check(res, {
        '201 응답': (r) => r.status === 201,
        // 요청한 포맷 수만큼 산출물이 나와야 한다. 일부만 만들어져도 201 이라 상태 코드로는 안 걸린다.
        '요청한 포맷 수만큼 생성': (r) =>
            r.status !== 201 || (r.json('reports') || []).length === REPORT_FORMATS.length,
    });
}
