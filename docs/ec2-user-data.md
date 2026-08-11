# EC2 시작 템플릿 user-data

ASG(`aivle-audit-core-asg`)가 인스턴스를 생성할 때 실행하는 부트스트랩 스크립트다.

배포는 인스턴스 갱신(instance refresh) 방식이라 **인스턴스는 배포마다 교체된다.** 인스턴스에 SSH로 접속해 수동으로 설치·수정한 내용은 다음 배포 때 모두 사라지므로, 기동에 필요한 모든 작업은 이 스크립트에 있어야 한다.

## 선결 조건

적용 전에 아래가 충족되어야 한다.

| 항목 | 상태 확인 방법 |
|---|---|
| `docker-compose.prod.yml`의 backend가 `image:` 참조 | develop 브랜치에 PR #297 병합 여부 |
| GHCR 패키지 public | 아래 [검증](#ghcr-접근-확인) 참고 |
| S3에 `backend.env` 존재 | `aws s3 ls s3://aivle-audit-storage-412322926481-ap-northeast-2-an/config/` |
| 인스턴스 IAM 역할에 해당 S3 객체 읽기 권한 | 역할 정책 확인 |

PR #297 병합 전에 이 스크립트를 적용하면, `git clone`이 가져오는 compose 파일에 아직 `build:`가 남아 있어 `pull`이 backend를 건너뛰고 `up -d`가 그 자리에서 Gradle 빌드를 수행한다. 기동 시간 단축 효과가 없다.

## 스크립트

시작 템플릿 → 고급 세부 정보 → 사용자 데이터에 아래를 붙여넣는다.

> **`#!/bin/bash`는 반드시 1행 1열에서 시작해야 한다.** 앞에 공백이나 빈 줄이 있으면 cloud-init이 셸 스크립트로 인식하지 못해 **아무것도 실행하지 않는다.** 이 경우 인스턴스는 정상 부팅되지만 Docker도 앱도 없는 빈 상태가 되고, EC2 상태 검사는 통과하므로 ASG는 정상으로 판정한다.

```bash
#!/bin/bash
set -euo pipefail

APP_DIR=/home/ubuntu/backend-repo
REPO_URL=https://github.com/kt-aivle-big-project-13/backend-repo.git
BRANCH=develop
ENV_S3_URI=s3://aivle-audit-storage-412322926481-ap-northeast-2-an/config/backend.env
AWS_REGION=ap-northeast-2
COMPOSE_VERSION=v2.32.4

# 부팅 직후 unattended-upgrades 가 dpkg 락을 점유하는 경우가 있다.
# set -e 상태에서 apt-get 이 실패하면 스크립트 전체가 중단되므로 해제를 기다린다.
wait_dpkg_lock() {
  for _ in $(seq 1 60); do
    fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 || return 0
    echo "waiting for dpkg lock..."
    sleep 5
  done
  echo "dpkg lock timeout" >&2
  return 1
}

wait_dpkg_lock
apt-get update
wait_dpkg_lock
apt-get install -y docker.io git awscli

systemctl enable --now docker
usermod -aG docker ubuntu

# compose 플러그인: 버전을 고정하고 아키텍처는 실행 환경에서 결정한다.
# latest 를 받으면 upstream 변경 시 신규 인스턴스가 일제히 실패할 수 있다.
ARCH=$(uname -m)
install -d /usr/local/lib/docker/cli-plugins
curl -fsSL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-${ARCH}" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 재실행 가능하도록 기존 디렉터리를 정리한 뒤 클론한다.
rm -rf "$APP_DIR"
git clone -b "$BRANCH" --depth 1 "$REPO_URL" "$APP_DIR"
cd "$APP_DIR"

# .env 에는 JWT_SECRET, DB_PASSWORD, OPENAI_API_KEY 등이 들어 있다.
# aws s3 cp 는 기본 644 로 파일을 생성하므로 권한을 좁힌다.
aws s3 cp "$ENV_S3_URI" "$APP_DIR/.env" --region "$AWS_REGION"
chmod 600 "$APP_DIR/.env"
chown -R ubuntu:ubuntu "$APP_DIR"

docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d

# 기동 실패를 로그에 남긴다. ALB 헬스체크와 별개로 원인 추적에 쓰인다.
for _ in $(seq 1 60); do
  if curl -fsS http://localhost:8080/actuator/health/readiness >/dev/null 2>&1; then
    echo "application is ready"
    exit 0
  fi
  sleep 5
done

echo "application did not become ready within 300s" >&2
exit 1
```

## 이전 버전에서 달라진 점

| 항목 | 이전 | 변경 | 이유 |
|---|---|---|---|
| 이미지 확보 | `up -d --build` | `pull` + `up -d` | EC2에서 Gradle 재빌드 제거. 기동 시간 5~15분 → 1분 내외 |
| `.env` 권한 | `644` (기본값) | `chmod 600` | 시크릿이 모든 로컬 계정에 노출됨 |
| compose 버전 | `latest` | `v2.32.4` 고정 | upstream 변경 시 전 인스턴스 동시 실패 방지 |
| 아키텍처 | `x86_64` 하드코딩 | `uname -m` | Graviton 전환 시 즉시 깨지는 문제 |
| 셸 옵션 | `set -euxo pipefail` | `set -euo pipefail` | `-x`는 실행 명령을 로그에 남긴다 |
| 브랜치 | 미지정 | `-b develop --depth 1` | 기본 브랜치 변경 시 다른 코드가 배포되는 문제, 클론 속도 |
| dpkg 락 | 미처리 | 해제 대기 | 부팅 직후 apt 실패로 인스턴스가 빈 채 뜨는 간헐적 장애 |
| curl 옵션 | `-SL` | `-fsSL` | `-f` 없으면 404 시 HTML 오류 페이지가 바이너리로 저장됨 |
| Docker 서비스 | 미지정 | `systemctl enable --now` | 재부팅 시 자동 시작 보장 |
| 기동 확인 | 없음 | readiness 폴링 | 실패 원인이 로그에 남음 |

`usermod -aG docker ubuntu`는 SSH 접속 후 `sudo` 없이 `docker` 명령을 쓰기 위한 편의 설정이다.

## 적용 방법

시작 템플릿은 버전을 수정할 수 없고 새 버전을 만들어야 한다.

1. EC2 → 시작 템플릿 → 해당 템플릿 선택
2. **작업 → 템플릿 수정(새 버전 생성)**
3. 고급 세부 정보 → 사용자 데이터에 위 스크립트 입력
4. 새 버전 생성 후, ASG가 참조하는 버전을 새 버전으로 변경
   (ASG 설정에서 시작 템플릿 버전이 `Latest`로 되어 있으면 자동 반영)
5. 인스턴스 갱신 실행 (develop에 push하면 CI가 자동 수행)

## 검증

### GHCR 접근 확인

익명 pull이 가능해야 인스턴스에서 인증 없이 이미지를 받을 수 있다.

```bash
TOKEN=$(curl -s "https://ghcr.io/token?scope=repository:kt-aivle-big-project-13/backend-repo:pull&service=ghcr.io" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
curl -s -o /dev/null -w "HTTP %{http_code}\n" -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.oci.image.index.v1+json" \
  "https://ghcr.io/v2/kt-aivle-big-project-13/backend-repo/manifests/develop"
```

`HTTP 200`이면 정상. `403`이면 패키지가 private 상태다.

### 인스턴스 기동 확인

새 인스턴스에 접속해 확인한다.

```bash
# user-data 실행 로그 (실패 시 원인이 여기 남는다)
sudo tail -50 /var/log/cloud-init-output.log

# 컨테이너 상태
sudo docker compose -f docker-compose.prod.yml ps

# 앱 응답
curl -s localhost:8080/actuator/health/readiness

# 사용 중인 이미지가 GHCR 것인지
sudo docker inspect audit-backend --format '{{.Config.Image}}'

# .env 권한이 600 인지
sudo ls -l /home/ubuntu/backend-repo/.env
```

### 모니터링 확인

Prometheus·Grafana 는 앱 인스턴스가 아니라 **ASG 외부 모니터링 전용 인스턴스**에서 구동된다(#307). 앱 인스턴스에는 `node-exporter` 만 남아 있다.

앱 인스턴스에서는 스크랩 대상 포트가 열려 있는지만 확인한다.

```bash
sudo docker compose -f docker-compose.prod.yml ps | grep node-exporter
curl -s localhost:8080/actuator/prometheus | head -3
```

타깃 상태는 모니터링 인스턴스에서 확인한다.

```bash
curl -s localhost:9090/api/v1/targets | grep -o '"health":"[^"]*"'
```

`spring-boot` 나 `node` 가 DOWN이면 앱 보안그룹에 모니터링 SG로부터의 8080·9100 인바운드가 있는지 먼저 확인한다.

## 롤백

특정 이미지로 고정하려면 `.env`에 태그를 지정한다. CI가 `sha-<단축해시>` 태그를 함께 푸시한다.

```
BACKEND_IMAGE_TAG=sha-8f8be34
```

S3의 `backend.env`를 수정한 뒤 인스턴스 갱신을 실행하면 해당 이미지로 배포된다. 미지정 시 기본값은 `develop`이다.

## 남은 과제

- **ASG 헬스체크가 EC2 유형이면** user-data가 실패해도 정상으로 판정된다. ELB 전환 및 대상 그룹 경로 `/actuator/health/readiness` 설정이 필요하다 (#299)
- **`MinHealthyPercentage: 0`** 설정으로 갱신 중 다운타임이 발생한다. 무중단이 목표라면 ASG 최대 용량 확보 후 100으로 상향해야 한다 (#299)
- **Prometheus·Grafana가 인스턴스마다 개별 기동**된다. 인스턴스가 2대 이상으로 늘면 각자 자기 자신만 수집하고, 갱신 시 수집 데이터와 대시보드가 함께 사라진다. 모니터링 스택 분리 검토 필요
- **이미지가 `linux/amd64` 단일**이다. Graviton 인스턴스로 전환하려면 CI에 멀티아키 빌드 추가가 선행되어야 한다
- **user-data와 이 문서의 동기화**는 수동이다. 시작 템플릿을 변경하면 이 문서도 함께 갱신해야 한다
