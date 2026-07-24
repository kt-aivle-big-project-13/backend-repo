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

---

## 2. k6 설치

이 프로젝트는 Spring Boot 앱을 로컬에서 직접 실행하고(`./gradlew bootRun`),
`docker-compose.yml`은 Postgres/Redis/Prometheus/Grafana/MinIO 같은 인프라만 띄우는 구조다.
k6도 앱과 같은 호스트에서 도는 게 자연스러우므로 **로컬 설치**를 기본으로 한다.

### macOS
​```
brew install k6
​```

### Windows
​```
winget install k6
​```

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
​```bash
k6 version
​```

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
python3 -c "import bcrypt; print(bcrypt.hashpw(b'password1234', bcrypt.gensalt()).decode())"
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
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"loadtest@example.com","password":"password1234","rememberMe":false}'
```

`accessToken`이 정상 발급되면, 그 토큰으로 대상 API를 한 번씩 호출해 200 응답과
데이터가 나오는지 확인한 뒤 k6 스크립트를 실행한다.

```bash
curl http://localhost:8080/api/v1/audits/1/explainability \
  -H "Authorization: Bearer {accessToken}"
```

---

## 5. 다음 단계

k6 스크립트 구성과 실행 방법은 `k6/` 폴더의 스크립트를 참고한다
(`scenarios/explainability.js`, `scenarios/fairness.js`, `scenarios/dataset-list.js`).