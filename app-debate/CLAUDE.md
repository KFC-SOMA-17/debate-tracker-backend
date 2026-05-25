# app-debate — CLAUDE.md

> 루트 [CLAUDE.md](../CLAUDE.md) 와 [docs/multi-module-strategy.md](../docs/multi-module-strategy.md) 를 먼저 읽고 와야 한다.

## 이 모듈이 책임지는 것 (F1 도메인, §5.1)

- **세션 lifecycle**: 토론 생성·시작·종료·재접속.
- **WebSocket broadcast**: 발화/쟁점/메타데이터 변경을 viewer 들에게 fan-out. NFR [PE-04](../docs/NFR.md), RE-01/02.
- **발화/쟁점 도메인 모델**: 발화(`Utterance`), 쟁점(`Issue`), 주장/근거 트리.
- **화자·찬반 라벨링**: STT speaker ID → 도메인 찬/반 매핑 (세션 메타데이터 + 발화 순서 기반). NFR [FS-03](../docs/NFR.md).
- **use case wrapper**: `UtteranceCorrector`(저비용 모델), `IssueTreeExtractor`(고품질 모델). 모두 `LlmClient` 를 감싸는 도메인 wrapper.
- **트리거 주기 제어**: 보정 15~30초 cycle, 쟁점추출 20~40초 cycle. raw chunk → 윈도우 buffering 도 여기.
- **DB 스키마 owner**: Flyway 마이그레이션 실행 권한은 이 앱만 가진다.

## 이 모듈이 책임지지 않는 것

- 외부 벤더 SDK 호출 자체 → `infra-stt` / `infra-llm` 에 위임.
- 보정/쟁점추출 결과의 **사후** 분석·세특·카드뉴스 → `app-report` 의 Sprint 3+ 영역.
- LLM 메트릭 emit (호출 latency·token·cost) → `infra-llm` 측 책임. 단 `RequestContext(sessionId, useCase)` 는 이 모듈이 채워서 넘겨야 라벨링이 된다.

## 의존성

`app-debate/build.gradle`:

```gradle
implementation project(':infra-stt')
implementation project(':infra-llm')
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
runtimeOnly 'com.h2database:h2'
```

- **`app-report` 의존 추가 금지** (§2.2).
- 벤더 SDK 직접 의존 금지 — `infra-*` 가 흡수한다.
- WebSocket starter, Redis (Pub/Sub 백플레인), SQS 클라이언트 등은 F1 구현 PR 에서 추가될 것.

## 부트스트랩

`com.debatetracker.debate.DebateApplication` — 표준 `@SpringBootApplication`. 다른 base 패키지 (`com.debatetracker.infra.*`) 의 빈을 스캔하려면 `@ComponentScan(basePackages = ...)` 또는 `infra-*` 의 auto-configuration import 가 필요할 수 있다 (§2.4 — `@ConditionalOnProperty` / `@Import` 활성화 권장).

## 코드 배치 가이드

```
app-debate/src/main/java/com/debatetracker/debate/
├── session/         # 세션 lifecycle (Entity, Service, Repository, Controller)
├── utterance/       # 발화 도메인 + UtteranceCorrector wrapper
├── issue/           # 쟁점 트리 + IssueTreeExtractor wrapper
├── ws/              # WebSocket handler / broadcaster
├── labeling/        # speaker → 찬/반 매핑
└── DebateApplication.java
```

(아직 미생성 — F1 PR 에서 결정. 위는 참고용 제안.)

## F1 구현 시 결정 포인트 (§6.1 Open Questions)

코드 작성 중 마주치면 사용자에게 묻기. 임의로 결정 금지:

- 보정·쟁점추출 트리거 주기를 어떻게 잴 것인지 (스케줄러 vs 발화 임계치 기반 vs 둘 다).
- broadcast 순서 보장 — 발화 ID 단조 증가 + 클라이언트 last-received-id 기반 재요청. NFR [RE-01](../docs/NFR.md).
- 중간 합류 viewer 의 초기 화면 ≤ 2초 (NFR [RE-02](../docs/NFR.md)) — Postgres 인덱스 + Redis 캐시 + WS 핸드셰이크 합산 예산.
- ArchUnit 규칙 위치 — 이 모듈 테스트에 두기 (§2.4).

## NFR 책임 매핑

| NFR | 카테고리 | 이 모듈에서 보장 |
|-----|---------|----------------|
| [PE-01](../docs/NFR.md) STT P95 ≤ 4초 | 종단 latency | BFF + WS broadcast 경로 (STT 자체는 infra-stt) |
| [PE-02](../docs/NFR.md) 보정 cycle 15~30초 / P95 ≤ 35초 | 트리거 주기 | `UtteranceCorrector` 스케줄러 |
| [PE-03](../docs/NFR.md) 쟁점 cycle 20~40초 / P95 ≤ 50초 | 트리거 주기 | `IssueTreeExtractor` 스케줄러 |
| [PE-04](../docs/NFR.md) viewer 30명 broadcast ≤ 300ms | 동시 처리 | WebSocket fan-out + Redis Pub/Sub |
| [RE-01](../docs/NFR.md) WebSocket 재접속 손실 0 | 정합성 | 단조 발화 ID + last-received-id |
| [RE-02](../docs/NFR.md) 중간 합류 ≤ 2초 | 초기 로드 | session snapshot API + Redis 캐시 |
| [FS-03](../docs/NFR.md) 찬/반 라벨링 Acc ≥ 0.85 | 도메인 정확도 | speaker → 찬/반 매핑 알고리즘 |

## 테스트

- 단위 테스트: 도메인 로직 (라벨링, 트리거 주기 계산, 발화 ID 단조성 검증).
- 통합 테스트: WebSocket 라이프사이클은 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Spring TestClient.
- `infra-*` 호출은 `@MockBean` 또는 `llm.mode=mock` / `stt.mode=mock` 프로파일로 격리 (§5.2).
- DB: 1차 H2, Sprint 진행 시 Testcontainers Postgres 로 전환.
