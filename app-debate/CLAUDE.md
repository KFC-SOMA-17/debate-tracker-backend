# app-debate — CLAUDE.md

> 루트 [CLAUDE.md](../CLAUDE.md) 를 먼저 읽고 와야 한다.

## 이 모듈이 책임지는 것

- **세션 lifecycle**: 토론 생성·시작·종료·재접속 (`domain/debate/`, `domain/session/`).
- **WebSocket broadcast**: 발화/쟁점/메타데이터 변경을 viewer 들에게 fan-out (`ws/`).
- **발화/쟁점 도메인 모델**:
  - 전사/발화: `domain/transcript/` — `SpeechSegment`, `RefinedSpeechSegment`
  - 쟁점 트리: `domain/agendaboard/` — `AgendaBoard`, `Agenda`, `Claim`, `Evidence`, `Stance`, `EvidenceType`
- **LLM 어댑터**: `client/` — `LlmUtteranceCorrectorAdapter`(보정), `LlmDebateAgendaAnalyzerAdapter`(쟁점추출). `infra-llm` 의 `LlmClient` 를 도메인 인터페이스로 감싼다.
- **STT 이벤트 수신**: `infra-stt` 가 발행하는 Spring Events (`@EventListener`) 를 받아 전사 데이터 처리.
- **주기적 트리거**: `scheduler/` — `TranscribeRefiningScheduler`(보정, 30초), `AgendaAnalyzeScheduler`(쟁점추출, 60초). 활성 세션 순회.
- **DB 스키마 owner**: `app-report` 는 read-only. 스키마 변경은 이 앱이 주도.

## 이 모듈이 책임지지 않는 것

- 외부 벤더 SDK 호출 자체 → `infra-stt` / `infra-llm` 에 위임.
- 보정/쟁점추출 결과의 **사후** 분석·세특·카드뉴스 → `app-report` 의 Sprint 3+ 영역.

## 의존성

`app-debate/build.gradle`:

```gradle
implementation project(':infra-stt')
implementation project(':infra-llm')
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
runtimeOnly 'com.mysql:mysql-connector-j'
// 테스트는 Testcontainers (ci 프로파일에서 실제 사용)
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:mysql'
```

- **`app-report` 의존 추가 금지**.
- 벤더 SDK 직접 의존 금지 — `infra-*` 가 흡수한다.
- WebSocket starter, Redis (Pub/Sub 백플레인), SQS 클라이언트 등은 F1 구현 PR 에서 추가될 것.

## 부트스트랩

`com.debatetracker.debate.DebateApplication` — 표준 `@SpringBootApplication`. `infra-*` 의 auto-configuration 은 자동 import 된다.

## 코드 배치 구조

```
app-debate/src/main/java/com/debatetracker/debate/
├── DebateApplication.java
├── client/                        # LLM 어댑터 (infra-llm 래핑)
│   ├── LlmUtteranceCorrectorAdapter.java
│   └── LlmDebateAgendaAnalyzerAdapter.java
├── controller/                    # REST API
│   ├── config/
│   ├── debate/
│   ├── transcript/
│   └── agendaboard/
├── domain/                        # 도메인 모델 + 리포지토리 인터페이스
│   ├── debate/                   # 토론 메타
│   ├── session/                  # 세션 메타
│   ├── transcript/               # 전사/발화 (SpeechSegment, RefinedSpeechSegment)
│   │   └── repository/
│   └── agendaboard/              # 쟁점 트리 (Agenda, Claim, Evidence)
│       └── repository/
├── event/                         # 도메인 이벤트
├── exception/                     # 앱 전용 예외
├── infrastructure/                # 인프라 구현
│   ├── config/
│   └── persistence/              # JPA/Redis/JDBC 구현
│       ├── jpa/
│       ├── redis/
│       ├── jdbc/
│       └── inmemory/
├── log/                           # 모니터링 지표 수집
│   ├── DebateLogger.java
│   └── annotation/               # @LogStompStart, @LogStompStop, @LogStompAudio, @LogStompException
├── scheduler/                     # 주기 트리거
│   ├── config/
│   ├── TranscribeRefiningScheduler.java    # 보정 (30초)
│   └── AgendaAnalyzeScheduler.java         # 쟁점추출 (60초)
├── service/                       # 비즈니스 로직
│   ├── debate/
│   ├── transcript/
│   ├── agendaboard/
│   └── dto/                      # 서비스 계층 DTO
│       ├── transcript/
│       └── agendaboard/
└── ws/                            # WebSocket
    ├── controller/               # STOMP 컨트롤러 (DebateStompController)
    ├── id/
    ├── message/
    ├── sender/
    └── session/                  # BroadcasterReconnectGrace (재연결 grace 기간 관리)
```

## 현재 구현된 주요 정책

- **트리거 주기**: `@Scheduled(fixedRate)` 기반. 보정 30초, 쟁점추출 60초. 향후 발화 임계치 기반 동적 트리거로 전환 가능.
- **STT 이벤트 수신**: `infra-stt` 의 Spring Events (`TranscribeEvent`) 를 `@EventListener` 로 수신. 벤더별 transport 차이 (Azure WebSocket 등) 는 `infra-stt` 가 흡수.
- **LLM 호출**: 동기 방식 (`LlmClient.refine()`, `extract()` 동기 반환). 어댑터 클래스 (`client/`) 에서 도메인 객체 변환.
- **테스트 격리**: `llm.mode=mock` / `stt.mode=mock` 프로파일로 실 API 호출 차단. Fake 구현은 `test/fixture/` 에 위치.

## 성능 관련 책임

- **STT → WebSocket 경로**: `infra-stt` 이벤트 수신 → 도메인 처리 → WebSocket broadcast. 종단 latency 최소화.
- **보정 주기**: `TranscribeRefiningScheduler` (30초 고정). 활성 세션 순회. 향후 동적 조정 가능.
- **쟁점추출 주기**: `AgendaAnalyzeScheduler` (60초 고정). 활성 세션 순회. 향후 동적 조정 가능.
- **WebSocket fan-out**: `ws/sender/` — Redis Pub/Sub 기반 다중 viewer 동시 전송.
- **중간 합류 초기 로드**: session snapshot API + Redis 캐싱.

## 테스트

- **단위 테스트**: 도메인 로직, 스케줄러 트리거 주기 검증.
- **통합 테스트**: WebSocket 라이프사이클은 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Spring TestClient.
- **`infra-*` 격리**: `@MockBean` 또는 `llm.mode=mock` / `stt.mode=mock` 프로파일 사용. Fake 구현은 `test/fixture/` (`FakeLlmClient`, `FakeSttClient`).
- **DB 전략**:
  - 메인 런타임: MySQL (로컬 Docker Compose, 포트 13306)
  - 테스트: **프로파일 분기**
    - `local` (기본): 떠 있는 MySQL(`localhost:13306/debate`) / Redis(`localhost:16379`) 접속
    - `ci`: Testcontainers (`SPRING_PROFILES_ACTIVE=ci`)
  - 컨테이너 설정: `config.TestcontainersConfiguration` (`@Profile("ci")`)
  - DB 정리: `DatabaseCleaner` (MySQL `TRUNCATE`)
  - 새 통합 테스트 베이스에는 `@Import(TestcontainersConfiguration.class)` 추가
  - `@DataJpaTest` 사용 시 `@AutoConfigureTestDatabase(replace = NONE)` 필수
