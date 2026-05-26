# infra-stt — CLAUDE.md

> 루트 [CLAUDE.md](../CLAUDE.md) 와 [docs/multi-module-strategy.md](../docs/multi-module-strategy.md) §5.1·§5.2 를 먼저 읽고 와야 한다.

## 이 모듈의 본질

**벤더 중립 STT 클라이언트 + 벤더별 어댑터.** AWS Transcribe Streaming, Azure Speech Realtime 등 외부 STT 서비스를 도메인 앱에 일관된 인터페이스로 노출한다.

`java-library` plugin — Spring Boot fat-jar 아님. `app-debate` 가 이 모듈을 implementation 으로 끌어다 쓴다.

`package-info.java`:
> Vendor-neutral STT client abstractions and adapter implementations.
> See docs/multi-module-strategy.md §2.4 — interface signatures must remain vendor-neutral.

## 책임 범위 (F1, §5.1)

- AWS Transcribe Streaming / Azure Speech Realtime 어댑터 구현.
- 스트리밍 청크(1~3초) 양방향 protocol — partial vs final transcript 정규화.
- 벤더 speaker label 정규화. (도메인 찬/반 매핑은 `app-debate` 가 적용 — 이 모듈은 raw speaker ID 만 다룬다.)
- 인증·크레덴셜 주입, 벤더별 retry/backoff (AWS `ThrottlingException`, Azure error codes).
- 스트림 단절 시 **same-vendor 재연결** (cross-vendor failover 는 F1 보류, §5.3).
- 메트릭 emit:
  - WER → NFR [FS-01](../docs/NFR.md)
  - DER → NFR [FS-02](../docs/NFR.md)
  - 호출 latency / 성공률

## 책임이 아닌 것

- 세션 lifecycle / WebSocket broadcast → `app-debate`.
- 화자 ID → 찬/반 도메인 매핑 → `app-debate` 의 `labeling` 영역.
- LLM 호출 → `infra-llm`.

## 절대 깨면 안 되는 규칙 (§2.4)

1. **벤더 SDK 타입을 public 시그니처에 노출 금지.** `software.amazon.awssdk.*`, Azure SDK 타입, `HttpHeaders` 류 모두 인터페이스에서 보이면 안 됨.
2. `infra-llm` 에 의존 금지 (`infra-stt ↔ infra-llm` 무의존).
3. `app-*` 에 의존 금지.

## 인터페이스 결정 포인트 (§5.2, §6.1)

F1 PR 에서 결정. 임의로 정하지 말고 사용자에게 묻는다:

- **`SttClient` 는 단발 호출이 아닌 양방향 스트림.** audio stream in / transcript event out. 벤더 transport 차이(AWS HTTP/2 bidi, Azure WebSocket)는 인터페이스에 노출 금지 — transport-neutral event stream.
- partial vs final transcript 어떻게 구분할지 (이벤트 타입 vs 플래그).
- speaker label 정규화 형태 (벤더별 ID 를 도메인 free 한 string/int 로 통일).
- `RequestContext(sessionId, useCase)` 파라미터 형태 — 메트릭 라벨링용.

## 빈 노출 방식 (§2.4)

- Spring `@Configuration` + `@Bean` auto-configuration. `app-debate` 가 `@Import` 또는 `@ConditionalOnProperty` 로 활성화.
- `auto-configuration` 패키지 명명 규칙은 §6.1 미정 — 임시 후보: `com.debatetracker.infra.stt.config`.
- mock profile 1차부터 제공: `@ConditionalOnProperty(name="stt.mode", havingValue="mock")` → fixture 응답 빈. 위치 (이 모듈 내 vs `*-test-fixtures` 모듈) 는 §6.1 미정.

## 의존성

`infra-stt/build.gradle`:

```gradle
plugins { id 'java-library' }
```

AWS SDK / Azure SDK / WebSocket 클라이언트 등은 F1 구현 PR 에서 추가. 단 **public API 시그니처에서는 보이지 않아야** 한다 (§2.4).

## 보류 항목 (§5.3) — 1차에서 하지 않는다

- STT cross-vendor failover (AWS↔Azure). speaker ID 안정성 문제로 제외. same-vendor 재연결까지만.
- 벤더 무중단 런타임 전환. `@ConditionalOnProperty` 기반 재시작 시 전환만.
- `infra-common` 추출. retry/circuit breaker 가 `infra-llm` 과 비슷해 보여도 각자 구현 (§1 중복 수용). 3곳 이상 반복 시 재논의.

## 테스트

- `SttClient` 인터페이스 계약 테스트 (mock 어댑터 vs 실 어댑터 동일 동작 검증).
- 벤더 어댑터별 통합 테스트는 실 API 호출 비용 부담 → 별도 프로파일 / 수동 트리거 권장.
- 인터페이스 격리 검증 — 어댑터 외부에서 벤더 specific 타입 import 0건 (향후 ArchUnit, §6.1).
