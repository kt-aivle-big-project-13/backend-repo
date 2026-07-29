# backend-repo
신용 평가 AI 규제준수 자동감사 플랫폼​ (백엔드 레포)

<br>

## 기술 스택

| 구분 | 스택 | 선택 이유 |
| --- | --- | --- |
| Language | Java 21 | LTS 버전, record/패턴 매칭 등 최신 문법 활용 |
| Framework | Spring Boot 4.0.7, Spring Web MVC, Spring Data JPA, Spring Security, Spring Validation | 검증된 생태계, 팀 전체가 익숙한 스택 |
| Database | PostgreSQL (+ pgvector) | pgvector로 법령 조항 임베딩·RAG 검색을 별도 벡터 DB 없이 처리 |
| Migration | Flyway | 스키마 변경 이력을 코드로 관리, 배포 환경 간 스키마 동기화 |
| Cache | Redis | 세션/토큰 등 휘발성 데이터의 빠른 조회 + 반복 조회되는 API 응답(설명가능성/공정성/데이터셋 조회 등) 캐싱 |
| Auth | JWT (jjwt) | Stateless 인증으로 서버 확장(scale-out) 용이 |
| Storage | AWS S3 (로컬은 MinIO) | 모델 아티팩트·보고서 파일의 저비용 대용량 저장, 로컬은 S3 호환 오브젝트 스토리지인 MinIO로 대체해 자격증명 없이 동일 코드로 개발 |
| API 문서화 | springdoc-openapi (Swagger UI) | 코드 기반 자동 문서화로 프론트와의 스펙 싱크 유지 |
| 모니터링 | Micrometer, Prometheus, Grafana | 감사 처리 지연·오류율 등 운영 지표 실시간 관찰 |
| 테스트 | JUnit 5, Mockito, Testcontainers | 실제 DB/Redis와 동일한 환경으로 통합 테스트 신뢰도 확보 |
| 부하 테스트 | k6 | Redis 캐싱 적용 전/후 API 응답 속도 개선을 수치로 비교 |
| Build | Gradle | Groovy/Kotlin DSL 기반의 유연한 빌드 스크립트 |
| CI/CD & Infra | GitHub Actions, Docker, Docker Hub, AWS EC2 | push 시 빌드~배포 자동화, 컨테이너로 배포 환경 일관성 확보 |

<br>

### SW 아키텍처 (AI/프론트/백엔드)
<img width="1731" height="908" alt="SW 아키텍쳐" src="https://github.com/user-attachments/assets/e2c2eaed-12bb-4453-b527-7390defd02ea" />

<br>

### 배포 아키텍처 (CI/CD)
<img width="1222" height="782" alt="image" src="https://github.com/user-attachments/assets/0b9f893e-19d4-4760-a214-32a0320e0469" />

GitHub에 push되면 GitHub Actions가 `./Dockerfile`(jdk 기반)로 이미지를 빌드해 Docker Hub에 push하고,
Actions가 ssh로 EC2에 접속해 방금 push한 이미지를 pull → 기동한다. DB 관련 컨테이너(PostgreSQL, Redis)는 EC2에 별도로 띄워둔다.

<br>

## Table 설명
- USERS → **사용자** (플랫폼에 로그인하는 은행 담당자)
- AI_MODELS → **AI 모델** (감사 대상으로 등록된 신용평가 모델)
- PRE_DIAGNOSES → **고영향 AI 사전진단** (진단 1회 실행 결과)
- DIAGNOSIS_ANSWERS → **사전진단 문항 응답** (예/아니오 응답 낱개)
- AUDITS → **감사** (모델 1회 감사 실행 건)
- XAI_RESULTS → **설명가능성 분석 결과** (SHAP 지표)
- FAIRNESS_RESULTS → **공정성 분석 결과** (Fairlearn 지표)
- SELF_CHECK_ANSWERS → **규제 자가점검 응답** (STEP4 예/아니오 체크)
- LAW_ARTICLES → **법령 조항** (RAG 검색용 AI 기본법 조문)
- AUDIT_LAW_MAPPINGS → **감사-법령 매핑** (감사 결과와 조항의 충족/미충족 연결)
- REPORTS → **보고서** (자동 생성 산출물 5종, 버전별)
- LAW_REVISIONS → **법령 개정 이력** (대시보드 개정 알림 피드)
- NOTIFICATIONS → **알림 발송 이력** (법령 개정·재감사 권고 SMS/이메일 발송 기록)
- OBJECTIONS → **고객 이의제기** (이의제기 대응문서 초안·승인·전달)

<br>

## ERD (ERDCloud 사용)
<img width="2160" height="1562" alt="ERD" src="https://github.com/user-attachments/assets/d64c112a-4698-442e-a5d0-d1502cf3eb08" />

`users`를 중심으로 `ai_models` → `audits`/`pre_diagnoses` → `xai_results`/`fairness_results`/`reports` 등으로 이어지는 감사 도메인과,
`law_articles`/`law_revisions` 기반 법령 추적 도메인, `objections`(이의신청) 도메인으로 구성되어 있다. 각 테이블 의미는 위 [Table 설명](#table-설명) 참고.

<br>

## 패키지 구조

도메인 주도(domain-driven) 방식의 패키지 구조를 따른다. 각 도메인은 `controller / dto / entity / repository / service / type` 하위 패키지를 가지며,
도메인에 속하지 않는 공통 요소는 `global`에 둔다.

```text
com.aivle13.fin_audit_ai
├── FinAuditAiApplication.java
├── domain/
│   ├── user/                  # 사용자(금융기관 담당자) 계정
│   │   ├── controller/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/            # UserEntity
│   │   ├── repository/
│   │   ├── service/
│   │   └── type/               # UserRole
│   ├── auth/                    # 로그인/회원가입/토큰 재발급 (AuthService, RefreshTokenService)
│   ├── model/                  # 감사 대상 AI 모델 (AiModelEntity, ModelType, ModelStatus)
│   ├── diagnosis/               # 고영향 여부 사전진단 (PreDiagnosisEntity, DiagnosisAnswerEntity, DiagnosisResult)
│   ├── audit/                   # 감사 진행/결과 (AuditEntity, XaiResultEntity, FairnessResultEntity, SelfCheckAnswerEntity, AuditRegulationMappingEntity)
│   ├── law/                     # 법령 조항/개정 추적 (LawArticleEntity, LawRevisionEntity)
│   ├── report/                  # 감사 보고서 (ReportEntity)
│   ├── notification/            # 알림 발송 이력 (NotificationEntity)
│   └── objection/               # 이의신청 (ObjectionEntity)
├── global/
│   ├── config/                  # SwaggerConfig, CorsConfig, SecurityConfig, S3Config
│   ├── entity/                  # BaseEntity (created_at/updated_at 공통 필드)
│   ├── exception/                # BusinessException, ErrorCode, ErrorResponse, GlobalExceptionHandler
│   │   └── {domain}/              # 도메인별 커스텀 예외
│   ├── jwt/                      # JWT 발급/검증, 인증 필터·예외 핸들러 (JwtProvider, JwtAuthenticationFilter)
│   ├── mail/                     # 이메일 인증 메일 발송 (MailService)
│   ├── s3/                       # 파일 업로드/삭제 (S3FileStorageService, dev 프로필은 MinIO로 자동 분기)
│   ├── ai/                       # AI 서버(FastAPI) 연동 클라이언트 (ShapAnalysisClient, FairnessAnalysisClient)
│   ├── util/                     # 공통 유틸 (EmailNormalizer 등)
│   └── validation/               # 커스텀 Bean Validation (PasswordValidator 등)
└── health/                       # 인프라(DB/Redis) 연결 확인용 헬스체크
```

각 계층의 역할:
- `controller`: HTTP 요청/응답만 다룬다. 검증된 DTO를 받아 service를 호출하고 결과를 DTO로 반환한다.
- `dto`: 외부(클라이언트)와 주고받는 데이터 형태. `entity`를 직접 노출하지 않는다.
- `entity`: JPA 매핑 클래스. `BaseEntity`를 상속하면 `created_at`/`updated_at`이 자동 관리된다(`@EnableJpaAuditing` 필요).
- `repository`: `entity` 단위의 데이터 접근.
- `service`: 트랜잭션 경계이자 도메인 로직이 위치하는 곳.
- `type`: 해당 도메인의 enum. 접미사 없이 의미 그대로 명명한다(`UserRole`, `AuditStatus` 등).

<br>

## DTO 구조 (record 사용 이유 및 작성 방법)

요청/응답 DTO는 모두 **Java record**로 작성한다.

<br>

### record를 쓰는 이유

- **불변성**: 모든 컴포넌트가 `final`이라, 요청을 받아 처리하는 도중 값이 바뀔 걱정이 없다. DTO는 "값을 옮기는 것"이 유일한 역할이므로 가변일 이유가 없다.
- **보일러플레이트 제거**: `equals`/`hashCode`/`toString`/getter가 컴파일러에 의해 자동 생성된다. Lombok의 `@Getter`/`@ToString`을 DTO에까지 붙일 필요가 없어진다.
- **의도가 드러나는 코드**: `class`로 선언하면 "로직이 있을 수도 있는 객체"처럼 보이지만, `record`는 "데이터 그 자체"라는 게 선언만 봐도 드러난다. DTO에 비즈니스 로직이 섞여 들어가는 것도 자연스럽게 막아준다.
- **compact constructor로 검증을 한 곳에 모음**: 필드마다 별도 setter 검증을 만들 필요 없이, 생성 시점에 한 번만 검증하면 이후로는 항상 유효한 상태임이 보장된다.

<br>

### 작성 방법

- 위치: `domain/{도메인}/dto/request`, `domain/{도메인}/dto/response`로 분리한다.
- 네이밍: `{동작}{도메인}Request` / `{도메인}Response` 형태를 기본으로 하되, 한 도메인에 응답 형태가 여러 개면 목적을 붙인다 (`UserResponse`, `UserSummaryResponse` 등).
- Request는 Bean Validation 어노테이션을 record 컴포넌트에 직접 붙인다.
- Response는 엔티티 → DTO 변환용 정적 팩토리 메서드(`from`/`of`)를 둔다. `ErrorResponse`(`global/exception/ErrorResponse.java`)가 이 패턴의 참고 예시다.
- 컴포넌트 유효성 자체를 생성 시점에 막아야 하면 compact constructor를 쓴다.

```java
// request
public record UserCreateRequest(
        @NotBlank String institution,
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank @Size(min = 8) String password
) {
}

// response
public record UserResponse(
        Long id,
        String institution,
        String name,
        String email
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getInstitution(), user.getName(), user.getEmail());
    }
}
```

<br>

## 테스트 방법

전체 테스트 실행:

```bash
./gradlew test
```

통합/시나리오 테스트는 Testcontainers로 PostgreSQL(pgvector)·Redis 컨테이너를 직접 띄우므로, 로컬에 **Docker가 실행 중이어야** 한다.

<br>

### 단위 테스트 (Unit Test)

- **대상**: Service의 도메인 로직처럼 Spring 컨텍스트 없이 순수하게 검증 가능한 코드.
- **도구**: JUnit 5 + Mockito(`@ExtendWith(MockitoExtension.class)`). 의존하는 Repository/외부 클라이언트는 `@Mock`으로 대체한다.
- **위치**: `src/test/java/.../domain/{도메인}/service` 등, 대상 클래스와 동일한 패키지.
- **특징**: Spring 컨텍스트/DB/Redis를 띄우지 않아 빠르다. 가능한 한 이 레벨에서 많은 케이스(정상/예외 흐름, 경계값)를 커버한다.

<br>

### 통합 테스트 (Integration Test)

- **대상**: Repository 쿼리, JPA 매핑, 여러 빈이 실제로 연동되는 지점.
- **도구**: `@SpringBootTest` + Testcontainers. `support.IntegrationTestSupport`를 상속하면 `PostgreSQLContainer`(pgvector/pgvector:pg16 이미지)와 Redis 컨테이너가 자동으로 기동되고, `@DynamicPropertySource`로 접속 정보가 주입된다.
- **위치**: `src/test/java/com/aivle13/fin_audit_ai/` 루트 또는 도메인 하위. 참고 예시: `InfraIntegrationTest`, `FinAuditAiApplicationTests`.
- **특징**: 실제 DB/Redis에 읽고 쓰며 동작을 검증한다. `@ActiveProfiles("test")`로 테스트 전용 설정을 사용한다.

```java
class SomeIntegrationTest extends IntegrationTestSupport {

    @Autowired SomeRepository someRepository;

    @Test
    @DisplayName("설명은 한글로, 검증하려는 동작을 문장으로 적는다")
    void someBehavior() {
        // given / when / then
    }
}
```

<br>

### 시나리오 테스트 (Scenario / E2E Test)

- **대상**: "모델 등록 → 사전진단 → 감사 생성 → 보고서 조회"처럼 여러 API를 순서대로 호출하는 사용자 흐름 전체. 단위/통합 테스트로는 계층 간 연동에서 생기는 문제를 못 잡는다.
- **도구**: `IntegrationTestSupport`를 상속해 실 DB/Redis를 사용하면서, `MockMvc`(또는 `TestRestTemplate`)로 컨트롤러 계층까지 포함해 API를 순서대로 호출한다.
- **네이밍**: `{흐름}ScenarioTest` (예: `AuditFlowScenarioTest`).
- **작성 원칙**: 각 단계의 응답으로 다음 단계 요청을 구성하고, 최종 상태(DB, 응답 바디)까지 검증한다. Mock으로 대체하는 대상은 AI 서버 등 외부 시스템 연동 정도로 최소화한다.

<br>

### 부하 테스트 (Load Test)

- **대상**: 반복 조회가 잦고 Redis 캐싱이 적용/예정인 API. 캐싱 적용 전/후 성능(응답 속도, 에러율) 개선을 수치로 비교한다.
  - `GET /api/v1/audits/{auditId}/explainability`
  - `GET /api/v1/audits/{auditId}/fairness`
  - `GET /api/models/{modelId}/datasets`
- **도구**: [k6](https://k6.io/). 앱은 `./gradlew bootRun`으로 로컬에서 직접 띄우고, k6도 같은 호스트에서 실행한다.
- **위치**: `k6/` 폴더. `config/env.js`(대상 URL·계정·리소스 ID), `lib/auth.js`(로그인 후 토큰 재사용), `scenarios/`(API별 시나리오: `explainability.js`, `fairness.js`, `dataset-list.js`).
- **사전 준비 및 실행 방법**: `docs/k6-load-test-setup.md` 참고 (k6 설치, 테스트용 계정/모델/데이터셋/감사 데이터 시드 순서 포함).

```bash
k6 run k6/scenarios/explainability.js
```

<br>

## 협업 규칙

### PR / 머지 규칙

- `main`, `develop` 브랜치에는 직접 push하지 않는다.
- 모든 변경 사항은 작업 브랜치에서 개발한 후 PR을 생성한다.
- 다른 팀원 1명 이상의 승인을 받아야 병합할 수 있다.
- 병합 완료 후 작업 브랜치는 삭제한다.

<br>

### 브랜치 전략

- `main`, `develop` 두 브랜치만 상시 운영한다.
- `main`에는 직접 push하지 않는다 (릴리즈 시에만 `develop → main` 병합).
- `develop`이 default 브랜치이며, 모든 작업 브랜치는 `develop`에서 분기하고 `develop`으로 병합한다.

<br>

### 브랜치 네이밍 규칙

`타입/#이슈번호-기능설명` 형식을 사용한다. (예: `feat/#22-model-upload`)

| 타입       | 설명                         |
| ---------- | ---------------------------- |
| `feat`     | 새로운 기능 개발             |
| `fix`      | 오류 수정                    |
| `refactor` | 기능 변경 없는 코드 개선     |
| `style`    | 코드 포맷, 들여쓰기 등 수정  |
| `chore`    | 환경설정                     |
| `infra`    | Docker, AWS, CI/CD 등 인프라 |
| `test`     | 테스트                       |
| `docs`     | 문서 작성 및 수정            |

<br>

### 커밋 메시지 컨벤션

형식: `[타입/#이슈번호] 메시지`

- 메시지는 모호하지 않고 상세하게 작성한다.
    - 좋은 예: `[fix/#155] 잘못된 비밀번호 입력 시 예외 응답 오류 수정`
    - 안좋은 예: `[fix/#155] 로그인 수정`
- 하나의 커밋엔 하나의 작업만 담는다.
    - 좋은 예: `[feat/#132] 모델 업로드 API 추가`
    - 안좋은 예: `[feat/#132] 모델 업로드 API 추가 및 로그인 오류 수정 및 CSS 변경`
- 제목 끝에 마침표를 붙이지 않는다.
    - 좋은 예: `[feat/#132] 비밀번호 입력 오류 수정`
    - 안좋은 예: `feat/#132: 비밀번호 입력 오류 수정.`
- 명사체로 작성한다.
    - 좋은 예: `[feat/#132] 비밀번호 입력 오류 수정`
    - 안좋은 예: `[feat/#132] 비밀번호 입력 오류 수정합니다.`

<br>

### API prefix 규칙

`/api/**` 형태로 데이터 요청 경로임을 명확히 구분한다. 시스템 관리와 업데이트를 쉽게 하기 위한 목적이다.

- `/api`: API 요청 경로임을 명시
- `/auth`: 인증 관련 기능
- `/login`: 세부 작업

규칙:

- 리소스명은 복수 명사를 사용한다.
- 동사 사용을 금지한다.
- 단어 구분이 필요하면 하이픈(`-`)을 사용한다.

<br><br>

## 로컬 파일 스토리지 (MinIO)

로컬 환경에는 실제 AWS 자격증명이 없으므로, `dev` 프로필에서는 `S3Config`가 S3 호환 오브젝트 스토리지인 [MinIO](https://min.io/)를 사용하도록 자동 분기된다(`prod`는 기존 AWS S3 그대로 사용). `S3FileStorageService` 등 파일 업로드/삭제 코드는 수정 없이 그대로 동작한다.

**1. 컨테이너 실행**

```bash
docker compose up -d minio
```

**2. 콘솔 접속** ([http://localhost:9001](http://localhost:9001))

`.env`의 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`로 로그인한다.

**3. 버킷 생성**

콘솔에서 `.env`의 `AWS_S3_BUCKET`과 동일한 이름으로 버킷을 하나 생성한다.

세팅이 끝나면 애플리케이션에서 파일을 업로드/삭제했을 때 MinIO 콘솔(Buckets → 해당 버킷)에서 객체가 바로 확인된다.

<br><br>

## Swagger 연동 확인
(http://localhost:8080/swagger-ui/index.html#)

<img width="1747" height="1228" alt="img" src="https://github.com/user-attachments/assets/805bb87a-1290-41b2-9900-44e3ee2aa6c7" />

<br><br>

## 모니터링 지표 (3가지)
Grafana (http://localhost:3001)

### 📝 JVM (Micrometer)
힙 사용량과 GC를 추적해 메모리 누수·OOM을 조기에 발견

<img width="1265" height="665" alt="image" src="https://github.com/user-attachments/assets/6470513c-0b06-4ab5-8191-6b2d5e09ae85" />


<br><br>

### 📝 Node Exporter Full
서버 하드웨어 자원(CPU·메모리·디스크)이 한계에 도달했는지 감시

<img width="1262" height="667" alt="image" src="https://github.com/user-attachments/assets/f8bd998a-0e5a-405e-a9ad-5b5636a174cd" />


<br><br>

### 📝 Spring Boot 3.x Statistics
API 트래픽·응답시간·에러율로 서비스가 정상 작동하는지 확인

<img width="1268" height="546" alt="image" src="https://github.com/user-attachments/assets/d7e41eb8-b17e-4131-a55b-649eb1adface" />




