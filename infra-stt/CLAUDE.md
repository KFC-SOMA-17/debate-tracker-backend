# infra-stt — CLAUDE.md

> 루트 [CLAUDE.md](../CLAUDE.md) 를 먼저 읽고 와야 한다.

## 이 모듈의 본질

**벤더 중립 STT 클라이언트 + 벤더별 어댑터.** AWS Transcribe Streaming, Azure Speech Realtime 등 외부 STT 서비스를 도메인 앱에 일관된 인터페이스로 노출한다.

`java-library` plugin — Spring Boot fat-jar 아님. `app-debate` 가 이 모듈을 implementation 으로 끌어다 쓴다.

**핵심 원칙**: 인터페이스 시그니처는 벤더 중립. 벤더 SDK 타입 노출 금지.

## 책임 범위

- **벤더 중립 인터페이스**: `SttClient` — `startStreaming()`, `sendAudioChunk()`, `stopStreaming()`, `isConnected()`.
- **라우터**: `SttClientRouter` — `SttClient` 구현체. 세션 생성/조회/삭제를 `SttSessionRepository` 에 위임하고, 오디오 전송을 `SttSession` 에 위임.
- **세션 추상화**: `router/SttSession`, `router/SttSessionCreator`, `router/SttSessionRepository` 인터페이스로 벤더 구현 격리.
- **Azure Speech 구현**: `session/azure/AzureSttSession` (WebSocket 연결·오디오 전송·재연결), `session/azure/AzureSessionCreator` (ConversationTranscriber 조립).
- **세션 저장소**: `repository/InMemorySttSessionRepository` — `SttSessionRepository` 의 InMemory 구현. `ConcurrentHashMap` 기반.
- **이벤트 발행**: 전사 결과를 **Spring Events** (`TranscribeEvent`) 로 `app-*` 에 전달. `app-*` 는 `@EventListener` 로 수신.
- **벤더 크레덴셜**: `config/AzureConfig` — Azure subscription key, region 등. 환경 변수 주입.
- **오디오 설정**: `config/AudioProperties` — PCM 16kHz, 1채널, 200ms 청크.
- **메트릭 로거**: `logger/SttLogger` — Micrometer `MeterRegistry` 기반. 현재 확장 예정.

## 패키지 구조

```
client/
  SttClient.java                     # 벤더 중립 인터페이스 (public API)
  dto/SttSegment.java                # 전사 결과 DTO
  event/TranscribeEvent.java         # Spring Event DTO

router/
  SttClientRouter.java               # SttClient 구현체 — 세션 생명주기 조율
  SttSession.java                    # 벤더 중립 세션 인터페이스
  SttSessionCreator.java             # 세션 생성 인터페이스
  SttSessionRepository.java          # 세션 저장소 인터페이스

session/azure/
  AzureSttSession.java               # Azure WebSocket 세션 구현
  AzureSessionCreator.java           # Azure ConversationTranscriber 조립

repository/
  InMemorySttSessionRepository.java  # SttSessionRepository InMemory 구현

config/
  SttAutoConfiguration.java          # 빈 등록 (@ConditionalOnProperty)
  AzureConfig.java                   # Azure 자격증명 설정
  AudioProperties.java               # 오디오 포맷 설정

logger/
  SttLogger.java                     # Micrometer 기반 메트릭 로거
```

## 책임이 아닌 것

- 세션 lifecycle / WebSocket broadcast → `app-debate`.
- 화자 ID → 찬/반 도메인 매핑 → `app-debate`.
- 전사 이벤트 수신 후 처리 (저장, 보정 트리거 등) → `app-debate` 의 `@EventListener`.
- LLM 호출 → `infra-llm`.

## 절대 깨면 안 되는 규칙

1. **벤더 SDK 타입을 public 시그니처에 노출 금지.** `software.amazon.awssdk.*`, Azure SDK 타입 (`com.microsoft.*`), `HttpHeaders` 류 모두 인터페이스에서 보이면 안 됨.
2. `infra-llm` 에 의존 금지 (`infra-stt ↔ infra-llm` 무의존).
3. `app-*` 에 의존 금지.
4. `router/` 패키지의 인터페이스(`SttSession`, `SttSessionCreator`, `SttSessionRepository`)는 벤더 중립 유지. Azure SDK 타입 참조 금지.

## 현재 인터페이스 정책

- **스트리밍 방식**: `SttClient` — audio chunk 전송 (`sendAudioChunk()`). 전사 결과는 **Spring Events** (`TranscribeEvent`) 로 발행.
- **이벤트 수신**: `app-*` 는 `@EventListener` 로 `TranscribeEvent` 수신. 벤더 transport 차이(Azure WebSocket 등)는 이 모듈이 흡수.
- **벤더 타입 격리**: `SttClient` 인터페이스 및 이벤트 DTO 에 Azure SDK 타입 노출 금지. 벤더 중립 DTO (`SttSegment`, `TranscribeEvent`) 사용.
- **세션 관리**: `sessionId` 기반 세션 추적. `SttClientRouter` 가 `SttSessionRepository` (현재 `InMemorySttSessionRepository`) 를 통해 세션별 WebSocket 연결 관리.

## 빈 노출 방식

- **자동 설정**: `config/SttAutoConfiguration` — `@ConditionalOnProperty(name="stt.azure.enabled", havingValue="true")` 기반.
- **SttClient 빈**: `SttClientRouter` 로 등록. Azure 활성화 시에만 노출.
- **SttSessionCreator 빈**: `AzureSessionCreator` 로 등록. Azure 활성화 시에만 노출.
- **SttSessionRepository 빈**: `InMemorySttSessionRepository` 항상 등록 (조건 없음).
- **Mock 구현**: `app-debate` 의 `test/fixture/FakeSttClient` — 테스트 격리용.
- **설정 위치**: `stt-config.yml` — `stt.azure.*`, `stt.audio.*`.

## 의존성

`infra-stt/build.gradle`:

```gradle
plugins { id 'java-library' }

dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'com.microsoft.cognitiveservices.speech:client-sdk:1.41.1'  // Azure Speech SDK
    // 향후 AWS Transcribe Streaming 등 추가 가능
}
```

**중요**: 벤더 SDK 타입 (`com.microsoft.cognitiveservices.*`, AWS SDK 등)을 **public API 시그니처에 노출 금지**.

## 향후 확장 가능성

- **멀티 벤더**: 현재 Azure Speech 단일 벤더. 새 벤더 추가 시 `SttSessionCreator` + `SttSession` 구현체만 추가하면 됨. `SttClientRouter` 변경 불필요.
- **재연결 로직**: 현재 same-vendor 재연결. cross-vendor failover 는 speaker ID 안정성 이슈로 보류.
- **`infra-common` 추출**: retry/circuit breaker 가 `infra-llm` 와 중복되면 공통 모듈 검토. 1차에서는 각자 구현.
- **SttLogger**: Micrometer 기반 메트릭 확장 예정.

## 테스트

- **라우터 계약 테스트**: `SttClientRouterTest` — `SttClientRouter` 의 세션 생성/오디오 전송/중단 동작 검증.
- **세션 저장소 테스트**: `InMemorySttSessionRepositoryTest` — 세션 저장/조회/삭제 검증.
- **실 API 테스트**: `AzureSttClientIntegrationTest` — 실 Azure Speech 호출. 비용 부담 → 별도 프로파일 / 수동 트리거.
- **Mock 테스트**: `app-debate` 의 `FakeSttClient` 사용. 로컬 개발·CI 에서 실 API 호출 차단.
- **인터페이스 격리 검증**: `router/` 인터페이스 외부에서 Azure SDK 타입 (`com.microsoft.*` 등) import 금지. 향후 ArchUnit 으로 강제 예정.
