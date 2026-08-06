# k6 부하테스트 준비 가이드

## 1. 목적

Redis 캐싱 적용 전/후 성능 개선을 수치로 비교하기 위해 k6로 부하테스트를 수행한다 (`test/#71`).
이 문서는 테스트를 실행하기 전에 필요한 **k6 설치**와 **테스트용 DB 데이터 준비** 방법을 정리한다.

대상 API:
- `GET /api/v1/audits/{auditId}/explainability`
- `GET /api/v1/audits/{auditId}/fairness`
- `GET /api/models/{modelId}/datasets`

세 API 모두 인증(JWT)이 필요하고, 요청한 사용자가 소유한 리소스만 조회할 수 있다
(`findByIdAndUser_Id` 패턴). 따라서 테스트 전에 **로그인 가능한 사용자 계정**과
**그 계정 소유의 모델/데이터셋/감사(완료 상태) 데이터**가 DB에 있어야 한다.

이후 추가된 **리포트 생성·챗봇**은 요청마다 LLM 을 호출해 부하 특성이 전혀 달라 시나리오와
준비 절차를 따로 둔다 — `6. 리포트 생성·챗봇 시나리오` 참고. 2~4 절의 계정·데이터 준비는
그대로 필요하다.

---

## 2. k6 설치

이 프로젝트는 Spring Boot 앱을 로컬에서 직접 실행하고(`./gradlew bootRun`),
`docker-compose.yml`은 Postgres/Redis/Prometheus/Grafana/MinIO 같은 인프라만 띄우는 구조다.
k6도 앱과 같은 호스트에서 도는 게 자연스러우므로 **로컬 설치**를 기본으로 한다.

### macOS
```bash
brew install k6
```

### Windows
```bash
winget install k6
```

### Linux (Debian/Ubuntu)
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6
```

### 설치 확인
```bash
k6 version
```

> **Git Bash(MINGW64) 사용 시 주의**: `winget install k6`로 설치해도 이미 열려 있던
> Git Bash 세션은 변경된 PATH를 못 읽어 `k6: command not found`가 뜰 수 있다.
> 새 터미널을 열어도 안 되면 아래처럼 직접 PATH를 추가한다.

```bash
find "/c/Program Files" -iname "k6.exe" 2>/dev/null   # 설치 경로 확인
echo 'export PATH="$PATH:/c/Program Files/k6"' >> ~/.bashrc
source ~/.bashrc
k6 version
```


> 참고: 팀원 간 환경을 완전히 통일하고 싶다면 `grafana/k6` Docker 이미지로 대체 실행할 수도 있다.
> 다만 컨테이너에서 호스트의 `localhost:8080`에 접근하려면 `--network=host`(리눅스 전용) 등
> 추가 네트워크 설정이 필요해 로컬 설치보다 번거롭다.

---

## 3. 사전 데이터 준비 (DB 시드)

`dev` 프로필은 `ddl-auto: create`라 앱을 한 번 기동하면 테이블이 자동 생성된다.
앱을 기동한 뒤, 아래 순서로 테스트용 데이터를 넣는다.

### 3-1. 테스트 계정 생성

`POST /api/v1/auth/signup`은 이메일 인증을 통과해야 하므로 부하테스트용 계정 생성에는
적합하지 않다. **비밀번호를 BCrypt로 직접 해시해서 DB에 바로 INSERT**한다.

BCrypt 해시 생성 (Python이 설치되어 있다면 가장 간단):
```bash
pip install bcrypt
python -c "import bcrypt; print(bcrypt.hashpw(b'password1234', bcrypt.gensalt()).decode())"
(안되면 python3 -c "import bcrypt; print(bcrypt.hashpw(b'password1234', bcrypt.gensalt()).decode())")
```

출력된 해시 값을 아래 SQL의 `password_hash`에 넣는다.

```sql
INSERT INTO users (name, institution, email, password_hash, role,
                    law_sms_enabled, reaudit_alert_enabled, created_at, updated_at)
VALUES ('부하테스트 유저', 'k6 테스트기관', 'loadtest@example.com',
        '$2a$10$여기에_생성한_BCrypt_해시_붙여넣기',
        'USER', true, true, now(), now())
RETURNING user_id;
```

`RETURNING user_id`로 나온 값을 아래 단계에서 `user_id`로 사용한다 (예시에서는 `1`).

### 3-2. 모델 등록

```sql
INSERT INTO ai_models (user_id, model_name, model_group_id, model_type, version,
                        domain, artifact_path, is_high_impact, status, created_at, updated_at)
VALUES (1, 'k6 테스트 모델', 'a1111111-1111-1111-1111-111111111111', 'XGBOOST', 'v1',
        'CREDIT_SCORING', 'models/k6-test/model.pkl', true, 'ACTIVE', now(), now())
RETURNING model_id;
```

### 3-3. 데이터셋 등록

`GET /api/models/{modelId}/datasets` 부하테스트 대상이므로 최소 1건 이상 필요하다.

```sql
INSERT INTO datasets (model_id, data_source, dataset_file_key, row_count, columns,
                       sensitive_attributes, audited, purpose, created_at, updated_at)
VALUES (1, 'DUMMY', NULL, 1000, 'age,income,gender,credit_score',
        'gender', true, 'AUDIT', now(), now())
RETURNING dataset_id;
```

### 3-4. 감사(Audit) 등록 — 완료 상태로

`explainability`/`fairness` 조회는 감사가 `PENDING`/`FAILED`가 아니어야 하고,
XAI 지표 3종이 모두 저장돼 있어야 정상 응답한다. 완료 상태로 바로 시드한다.

```sql
INSERT INTO audits (model_id, dataset_id, validation_dataset_id, assessment_id, user_id,
                     audit_name, sensitive_features, current_step, threshold_method,
                     target_approval_rate, manual_threshold, status,
                     completed_at, retention_until, created_at, updated_at)
VALUES (1, 1, NULL, NULL, 1,
        'k6 부하테스트용 감사', 'gender', 4, 'MANUAL',
        NULL, 0.5, 'COMPLIANT',
        now(), (now() + interval '5 years')::date, now(), now())
RETURNING audit_id;
```

### 3-5. XAI(설명가능성) 결과 3종 시드

```sql
INSERT INTO xai_results (audit_id, metric_code, value, threshold, status)
VALUES
  (1, 'SENSITIVE_CONTRIB', 0.1200, 0.2000, 'PASS'),
  (1, 'GLOBAL_STABILITY',  0.8500, 0.7000, 'PASS'),
  (1, 'FIDELITY',          0.9100, 0.8000, 'PASS');
```

### 3-6. 공정성(Fairness) 결과 시드

```sql
INSERT INTO fairness_results (audit_id, attribute, metric_code, value, threshold, status)
VALUES
  (1, 'gender', 'DEMOGRAPHIC_PARITY', 0.0500, 0.1000, 'PASS'),
  (1, 'gender', 'EQUALIZED_ODDS',     0.0700, 0.1000, 'PASS');
```

> 위 SQL의 `1`은 각 단계에서 `RETURNING`으로 나온 실제 ID로 바꿔서 사용한다.

---

## 4. 준비 확인

시드가 끝나면 로그인부터 확인한다.

```bash
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"loadtest@example.com","password":"password1234","rememberMe":false}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")

echo "$ACCESS_TOKEN"
```

`ACCESS_TOKEN`이 비어 있지 않으면, 그 토큰으로 대상 API를 한 번씩 호출해 200 응답과
데이터가 나오는지 확인한 뒤 k6 스크립트를 실행한다. `1`은 `3-4`에서 `RETURNING`으로 얻은
실제 `audit_id`로 바꿔서 사용한다.

```bash
curl http://localhost:8080/api/v1/audits/1/explainability \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## 5. 다음 단계

k6 스크립트 구성과 실행 방법은 `k6/` 폴더의 스크립트를 참고한다
(`scenarios/explainability.js`, `scenarios/fairness.js`, `scenarios/dataset-list.js`).

`config/env.js`의 기본값은 `--env`로 덮어쓸 수 있다. 시드한 데이터의 실제 `user_id`/`audit_id`에 맞춰
아래처럼 값을 넘겨 실행한다.

```bash
# GET /api/v1/audits/{auditId}/explainability
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env TEST_EMAIL=loadtest@example.com \
  --env TEST_PASSWORD=password1234 \
  --env AUDIT_ID=1 \
  k6/scenarios/explainability.js

# GET /api/v1/audits/{auditId}/fairness
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env TEST_EMAIL=loadtest@example.com \
  --env TEST_PASSWORD=password1234 \
  --env AUDIT_ID=1 \
  k6/scenarios/fairness.js

# GET /api/models/{modelId}/datasets
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env TEST_EMAIL=loadtest@example.com \
  --env TEST_PASSWORD=password1234 \
  --env MODEL_ID=1 \
  k6/scenarios/dataset-list.js
```

세 스크립트는 각각 별도로 실행해 시나리오별로 결과를 비교한다. `AUDIT_ID`/`MODEL_ID`는
`3-4`/`3-2`에서 `RETURNING`으로 얻은 실제 ID로 바꿔서 사용한다.

---

## 6. 리포트 생성·챗봇 시나리오

위 세 시나리오는 Redis 캐싱 대상인 가벼운 조회 API 기준이다. 리포트 생성과 챗봇은
**요청 한 건마다 LLM 을 호출**해 부하 특성이 전혀 달라, 시나리오도 따로 둔다.

| 스크립트 | 대상 | 실행 방식 | 임계값 |
|---|---|---|---|
| `scenarios/chat-history.js` | 대화 목록·이력 조회 | 램프업 최대 100 VU | `http_req_duration` p(95)<500ms (API 별로 분리) |
| `scenarios/chat-ask.js` | 질문(RAG + LLM) | VU 5 × 반복 8 | `chat_answer_duration` p(95)<8s |
| `scenarios/report-generation.js` | 산출물 생성(LLM + 문서 + S3) | VU 3 × 반복 3 | `report_generation_duration` p(95)<120s |

뒤의 둘은 기본 지표(`http_req_duration`)가 아니라 **성공 응답만 담는 전용 지표**에 임계값을
건다. 429(쿼터 소진)나 오류 응답은 LLM 을 타지 않아 빠르게 떨어지는데, 기본 지표에 섞이면
p(95) 를 끌어내려 실제로 느려져도 통과하기 때문이다. 같은 이유로 응답 본문 검증(빈 답변,
포맷 수 부족)도 `check()` 가 아니라 임계값이 걸린 지표(`chat_answer_empty`,
`report_incomplete`)로 센다 — k6 는 `check()` 실패로는 종료 코드를 바꾸지 않는다.

`chat-history.js` 는 한 반복에서 목록과 이력을 모두 부르므로 `endpoint` 태그로 나눠 잰다.
합쳐서 재면 한쪽이 기준을 넘어도 다른 쪽에 희석돼 안 걸린다.

조회 경로(`chat-history.js`)만 기존 램프업 패턴을 그대로 쓴다. 나머지 둘은 병목이 웹 계층이
아니라 AI 서버 응답이라, VU 를 올려도 큐만 길어지고 알 수 있는 게 없다. 대신 **반복 횟수를
고정해 호출 수와 비용을 예측 가능하게** 만든다.

### 6-1. 실행 전 확인

**⚠️ 두 시나리오는 부작용이 있다.** 실행하면 실제로 LLM 토큰을 쓰고, 리포트 생성은
S3(MinIO) 에 파일을 올리고 `reports` 행을 남긴다. 운영 환경에 걸지 말고, 반복 횟수를
먼저 확인한 뒤 실행한다.

**챗봇은 대화가 미리 있어야 한다.** 시나리오가 매 반복마다 대화를 새로 만들면 재려는
질문 경로가 아니라 대화 생성까지 같이 재게 되므로, 대화는 한 번만 만들고 그 ID 를 넘긴다.

```bash
CONVERSATION_ID=$(curl -s -X POST http://localhost:8080/api/v1/audits/1/conversations \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"부하테스트"}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['conversationId'])")

echo "$CONVERSATION_ID"
```

**일일 질문 수 제한을 확인한다.** `app.chat.daily-question-limit` 이 사용자·감사당 하루
몇 건인지 정한다(기본 50). 기본 반복 횟수(5 × 8 = 40건)는 이 안에 들어오지만, 늘려서 잴
때는 서버를 띄울 때 함께 올려야 한다. 안 올리면 51번째 요청부터 전부 429 다.

```bash
APP_CHAT_DAILY_QUESTION_LIMIT=1000 ./gradlew bootRun
```

시나리오는 429 를 실패로 세지 않고 `chat_quota_exceeded` 지표로 따로 집계한다. 실행 후
요약에서 이 값이 0 이 아니면 그만큼은 LLM 을 타지 않은 요청이므로, 그만큼 표본이 줄었다고
보면 된다. 응답시간 임계값은 애초에 201 응답만 담는 `chat_answer_duration` 에 걸려 있어
429 가 섞여도 왜곡되지 않는다.

**리포트 생성은 감사 결과가 있어야 한다.** `3-5`·`3-6` 의 XAI·공정성 결과가 없으면
빈 본문으로 리포트가 만들어져 측정값이 실제보다 짧게 나온다.

### 6-2. 실행

```bash
# 대화 목록·이력 조회 (LLM 없음, 램프업)
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env TEST_EMAIL=loadtest@example.com \
  --env TEST_PASSWORD=password1234 \
  --env AUDIT_ID=1 \
  --env CONVERSATION_ID=1 \
  k6/scenarios/chat-history.js

# 질문 (RAG + LLM). CHAT_VUS × CHAT_ITERATIONS 만큼만 호출한다
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env TEST_EMAIL=loadtest@example.com \
  --env TEST_PASSWORD=password1234 \
  --env CONVERSATION_ID=1 \
  --env CHAT_VUS=5 \
  --env CHAT_ITERATIONS=8 \
  k6/scenarios/chat-ask.js

# 산출물 생성 (LLM + 문서 변환 + S3). 기본 3 × 3 = 9건
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env TEST_EMAIL=loadtest@example.com \
  --env TEST_PASSWORD=password1234 \
  --env AUDIT_ID=1 \
  --env REPORT_FORMATS=PDF \
  --env REPORT_VUS=3 \
  --env REPORT_ITERATIONS=3 \
  k6/scenarios/report-generation.js
```

### 6-3. 무엇을 볼 것인가

- **리포트 생성**: 포맷을 여러 개 요청해도 LLM 본문은 한 번만 만들고 문서 변환만 포맷 수만큼
  돈다(`ReportGenerationService.generate`). `REPORT_FORMATS=PDF` 와 `REPORT_FORMATS=PDF,WORD`
  를 비교하면 문서 변환 비용만 분리해서 볼 수 있다.
- **챗봇**: `chat_answer_duration` 의 p(95) 가 AI 서버 응답 시간에 얼마나 붙어 있는지 본다.
  둘의 차이가 벌어지면 병목이 AI 서버가 아니라 RAG 검색(pgvector) 이나 영속화 쪽이다.
- 두 시나리오 모두 VU 를 올렸을 때 응답시간이 **선형으로 늘면 큐가 쌓이는 것**이고,
  꺾이면 그 지점이 동시 처리 한계다.