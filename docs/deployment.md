# 배포 가이드 (CD)

`app-debate` 의 dev 환경 배포 파이프라인 운영 문서. 전체 로드맵은 (1) CD 파이프라인 → (2) ElastiCache →
(3) RDS → (4) EC2 → (5) secret 채우고 배포 테스트 순이며, 이 문서는 (1) 의 산출물을 설명한다.

## 배포 흐름

```
GitHub Actions (develop push / 수동 workflow_dispatch)
  └─ dev-secret.yml 생성 (DEV_SECRET_YML → app-debate/src/main/resources/dev-secret.yml)
  └─ app-debate 이미지 빌드 (app-debate/Dockerfile, multi-stage) — dev-secret.yml 이 이미지에 포함됨
  └─ Docker Hub push        (debatetracker/debate-tracker-repo:latest, :<git-sha>)
  └─ SSH → EC2
        └─ docker-compose.dev.yml 전달 (scp)
        └─ .env 에 IMAGE 태그만 기록
        └─ docker compose pull && up -d   (SPRING_PROFILES_ACTIVE=dev)
        └─ /monitoring/health 로 배포 검증
```

- 배포 대상은 **`app-debate` 단일 컨테이너**. `infra-stt`/`infra-llm`/`common` 은 함께 패키징된다.
- DB/Redis 는 컨테이너가 아니라 **RDS / ElastiCache** 를 바라본다.
- 앱 비밀정보(DB/Redis/Gemini/Azure/CORS)는 **빌드 시 `dev-secret.yml` 로 이미지에 구워진다**(runtime env 주입 아님).

> ⚠️ 비밀이 이미지에 포함되므로 **Docker Hub repo 는 private 권장**. public 이면 이미지를 받는 누구나 값을 볼 수 있다.

## 트리거

- `develop` 브랜치 push (단, `**.md`, `docs/**`, `.claude/**` 등 변경만이면 skip)
- GitHub Actions 화면에서 **Run workflow** (수동, `workflow_dispatch`)

## 비밀정보 관리 방식: `dev-secret.yml`

GitHub Secret 변수를 최소화하기 위해, 앱이 쓰는 모든 비밀값을 **YAML 한 덩어리**로 모아
GitHub Secret `DEV_SECRET_YML` 하나에 보관한다.

- `application-dev.yml` 은 `spring.config.import: classpath:dev-secret.yml` 로 이 파일을 읽고,
  `${secret.*}` placeholder 로 값을 참조한다.
- CD 의 `Setting dev-secret.yml` step 이 빌드 직전 `DEV_SECRET_YML` 내용을
  `app-debate/src/main/resources/dev-secret.yml` 에 쓴다. Dockerfile 의 `COPY . .` 로 이미지에 포함된다.
- 실제 `dev-secret.yml` 은 **커밋 금지**(`.gitignore`). 구조는 `dev-secret.yml.example` 참고.

`DEV_SECRET_YML` 에 넣을 내용 (= `dev-secret.yml`):
```yaml
secret:
  datasource:
    host: debate-dev.xxxx.ap-northeast-2.rds.amazonaws.com   # RDS 엔드포인트
    port: 3306
    database: debate
    username: debate
    password: ********
  redis:
    host: debate-dev.xxxx.cache.amazonaws.com                # ElastiCache 엔드포인트
    port: 6379
  google:
    api-key: ********                                         # Gemini API 키
  azure:
    subscription-key: ********                               # Azure Speech 키
  cors:
    origin: https://dev.debate-tracker.com                   # 콤마 구분 다중 origin 가능
```

`application-dev.yml` 매핑:
- `spring.datasource.*` ← `secret.datasource.*`
- `spring.data.redis.*` ← `secret.redis.*`
- `spring.ai.google.genai.api-key` ← `secret.google.api-key`
- `stt.azure.subscription-key` ← `secret.azure.subscription-key` (+ `stt.azure.enabled: true` 고정)
- `cors.origin-urls` ← `secret.cors.origin`
- `AZURE_REGION` 은 `config-stt.yml` 기본값 `koreacentral` 사용.

## 필요한 GitHub Secrets

Settings → Secrets and variables → Actions 에 등록한다. dev 환경 secret 은 `DEV_` 접두사를 붙인다.

| Secret | 용도 | 채우는 단계 |
|--------|------|-------------|
| `DEV_SECRET_YML` | 앱 비밀정보 전체 (위 dev-secret.yml 내용) | 2·3·5단계 |
| `DEV_DOCKERHUB_USERNAME` | Docker Hub 로그인 계정 (`debatetracker` 네임스페이스 push 권한 필요) | 1단계 |
| `DEV_DOCKERHUB_TOKEN` | Docker Hub Access Token (push/pull) | 1단계 |
| `DEV_EC2_HOST` | EC2 퍼블릭 IP/도메인 | 4단계 |
| `DEV_EC2_USER` | SSH 계정 (예: `ubuntu`, `ec2-user`) | 4단계 |
| `DEV_EC2_SSH_KEY` | SSH 개인키 (PEM 전문) | 4단계 |

> 앱 비밀(DB/Redis/Gemini/Azure/CORS)은 개별 secret 으로 두지 않고 `DEV_SECRET_YML` 하나로 통합한다.
> DOCKERHUB / EC2 secret 은 GitHub Actions 러너·SSH 가 쓰는 값이라 yml 에 못 넣고 개별 secret 으로 둔다.

## EC2 의 `.env`

`.env` 는 repo 에 커밋하지 않는다. CD 가 배포마다 EC2 의 `~/app-debate/.env` 에 **`IMAGE` 한 줄만** 쓴다.
`docker-compose.dev.yml` 의 `image: ${IMAGE}` 변수 보간용이다 (앱 비밀은 이미 이미지에 포함되어 있어 불필요).
```dotenv
IMAGE=debatetracker/debate-tracker-repo:<git-sha>
```

## EC2 사전 준비 (4단계에서 수행)

1. Docker / Docker Compose plugin 설치
   ```bash
   sudo apt-get update && sudo apt-get install -y docker.io docker-compose-plugin
   sudo usermod -aG docker $USER   # 재로그인 필요
   ```
2. 배포 디렉토리는 SSH 계정 홈의 `app-debate/` (CD 의 scp target). 별도 생성 불필요 — scp 가 만든다.
3. 보안그룹 인바운드: 8080(앱), 8083(actuator, 필요 시), 22(SSH). RDS/ElastiCache 보안그룹은 EC2 SG 를 허용.

## dev 프로파일

`app-debate/src/main/resources/application-dev.yml`:
- DB/Redis/Gemini/Azure/CORS 접속값을 `${secret.*}`(= `dev-secret.yml`) 로 받는다.
- `jpa.hibernate.ddl-auto: update` — dev 초기 편의용 스키마 자동 반영. (운영 승격 시 Flyway 도입 검토)
- `config-monitoring-dev.yml` import → `/monitoring/health`, `/monitoring/prometheus` 노출.
- `stt.azure.enabled: true` 고정 — 꺼두면 `SttClient` 빈 부재로 부팅 실패.

## 롤백

이전 커밋 sha 태그로 이미지를 되돌린다.
```bash
cd ~/app-debate
sed -i 's#^IMAGE=.*#IMAGE=debatetracker/debate-tracker-repo:<previous-sha>#' .env
docker compose -f docker-compose.dev.yml pull
docker compose -f docker-compose.dev.yml up -d
```

## 로컬에서 dev 프로파일 점검

`dev-secret.yml` 을 만들고(예시 복사) 로컬 MySQL(13306)/Redis(16379) 가 떠 있는 상태에서 실행:
```bash
cd app-debate/src/main/resources
cp dev-secret.yml.example dev-secret.yml   # 값 채우기 (로컬 기본값이 이미 들어 있음)
cd -
SPRING_PROFILES_ACTIVE=dev ./gradlew :app-debate:bootRun
```
`dev-secret.yml` 은 gitignore 대상이라 커밋되지 않는다.

## 부록: 재연결 유예(`BroadcasterReconnectGrace`) 경합 이슈

코드 리뷰에서 지적된 동시성 버그를 정리한다. 대상은
`app-debate/.../ws/session/BroadcasterReconnectGrace.java` 다.

### 이 클래스가 하는 일

뷰어가 끊겼다고 토론을 바로 끝내지 않고 **40초 유예**를 둔다. 그 안에 재연결하면 종료를 취소하고,
재연결이 없으면 예약된 종료 콜백(`termination`)이 실행된다.

- `scheduleTermination(debateId, …)` — 40초 뒤 종료를 예약하고, 이전 예약이 있으면 취소한다.
  예약 핸들(`ScheduledFuture`)을 `pendingTerminations` 맵에 `debateId` 키로 보관한다.
- `cancel(debateId)` — 재연결 시 맵에서 예약을 꺼내 취소한다.
- `runTermination(...)` — 유예가 끝나 실제로 종료를 실행하는 콜백. 맵에서 자기 항목을 지운 뒤 종료를 실행한다.

### 무엇이 문제인가 (경합 시나리오)

핵심 원인은 `runTermination` 의 **키만 보고 지우는(key-only) `remove(debateId)`** 다.
이 콜백은 "내가 맵에 넣어둔 그 예약"이 아니라 **현재 그 키에 들어 있는 무엇이든** 지운다.

스케줄러는 콜백이 "정확히 40초"에 실행됨을 보장하지 않는다(스레드 풀 지연·GC 등으로 늦게 실행될 수 있다).
이 지연과 새 예약이 겹치면 다음이 벌어진다.

```
t0  끊김 → scheduleTermination → 예약 A 등록, map[id] = A
t1  40초 도달, A 콜백이 실행 큐에 올라가지만 살짝 지연됨(아직 remove 전)
t2  재연결 후 다시 끊김 → scheduleTermination 재호출
      → 예약 B 등록, cancelFuture(map.put(id, B)) 로 A 를 취소 시도
        (그러나 A 는 이미 발화돼 cancel 효과 없음), map[id] = B
t3  지연됐던 A 콜백이 드디어 remove(id) 실행
      → 키만 보고 지우므로 최신 예약 B 항목이 맵에서 사라진다
      → 이어서 A 가 termination.run() 실행 → 재연결했는데도 조기 종료!
t4  이후 누가 cancel(id) 를 호출해도 B 는 이미 맵에 없어 취소 불가
      → 살아남은 B 가 나중에 또 종료를 실행 (이중/유령 종료)
```

정리하면 두 가지 손상이 동시에 난다.

1. **최신 예약 유실** — 늦게 실행된 옛 콜백(A)이 최신 예약(B)의 맵 항목을 지운다.
2. **조기/유령 종료** — 재연결로 살아 있어야 할 토론이 끊기고, 추적에서 빠진 B 때문에 `cancel` 도 먹지 않는다.

실시간 토론 도중 재연결이 정상인데도 방송이 끊기는, 재현이 어렵고 사용자 영향이 큰 버그다.

### 어떻게 해결하나

콜백이 **자기가 등록한 바로 그 예약일 때만** 맵에서 지우도록 바꾼다.
`ConcurrentHashMap.remove(key, value)` 는 "키의 현재 값이 내가 기대한 값과 같을 때만" 원자적으로 지운다.
콜백은 자기 자신의 `ScheduledFuture` 를 알아야 하므로 `AtomicReference` 로 핸들을 전달한다.

```java
public void scheduleTermination(String debateId, Runnable termination) {
    AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
    ScheduledFuture<?> scheduled = taskScheduler.schedule(
            () -> runTermination(debateId, termination, self.get()),
            Instant.now().plus(RECONNECT_GRACE));
    self.set(scheduled);
    cancelFuture(pendingTerminations.put(debateId, scheduled));
}

private void runTermination(String debateId, Runnable termination, ScheduledFuture<?> expectedFuture) {
    if (!pendingTerminations.remove(debateId, expectedFuture)) {
        return; // 이미 최신 예약으로 교체됨 — 이 콜백은 더 이상 유효하지 않으니 아무것도 안 함
    }
    termination.run();
}
```

- 늦게 실행된 옛 콜백 A 는 `remove(id, A)` 가 실패한다(맵엔 이미 B). → **B 를 건드리지 않고 그냥 빠진다.**
- 따라서 A 는 `termination.run()` 도 호출하지 않는다. → **조기 종료 없음.**
- B 는 맵에 그대로 남아 있어 이후 `cancel(id)` 로 정상 취소된다. → **유령 종료 없음.**

### 개선 효과

- 같은 `debateId` 에 재연결 유예가 빠르게 갈아끼워져도 **항상 최신 예약 하나만** 유효하게 유지된다.
- "취소했는데 종료된다 / 재연결했는데 끊긴다" 류의 경합 버그가 구조적으로 사라진다(compare-and-remove 로 원자성 확보).
- 추가 락 없이 `ConcurrentHashMap` 의 원자 연산만 쓰므로 실시간 경로에 성능 부담이 없다.
