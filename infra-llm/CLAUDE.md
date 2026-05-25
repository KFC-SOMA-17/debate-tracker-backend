# infra-llm — CLAUDE.md

> 루트 [CLAUDE.md](../CLAUDE.md) 와 [docs/multi-module-strategy.md](../docs/multi-module-strategy.md) §5.1·§5.2 를 먼저 읽고 와야 한다.

## 이 모듈의 본질

**벤더 중립 LLM 클라이언트 + 벤더별 어댑터.** OpenAI, Anthropic, Google 등 외부 LLM 서비스를 도메인 앱에 일관된 인터페이스로 노출한다.

`java-library` plugin. `app-debate` 와 `app-report` 양쪽이 implementation 으로 끌어다 쓴다.

`package-info.java`:
> Vendor-neutral LLM client abstractions and adapter implementations.
> See docs/multi-module-strategy.md §2.4 — interface signatures must remain vendor-neutral.

## 책임 범위 (F1 한정, §5.1)

- 다중 모델군 어댑터:
  - 보정용 저비용 모델 (GPT-4o-mini / Claude Haiku / Gemini Flash 등)
  - 쟁점추출용 고품질 모델 (GPT-4o / Claude Sonnet / Gemini Pro 등)
- 벤더별 retry/backoff (OpenAI 429 + `Retry-After`, Anthropic `overloaded_error` 등).
- Circuit breaker per vendor endpoint.
- 단발성 호출 벤더 failover (예: OpenAI 지속 실패 시 Anthropic fallback).
- Rate limit 토큰버킷 (벤더 quota 보호).
- 벤더 특화 최적화 — Anthropic prompt caching 헤더 등.
- PII 마스킹된 request/response 로깅.
- 메트릭 emit:
  - 토큰 카운팅·비용 → NFR [PE-05](../docs/NFR.md)
  - 호출 성공률/지연 → NFR [FS-04/05/06](../docs/NFR.md) (도메인 정답셋 기반 일치율은 별도 평가셋 측정)

F2/F3 (Sprint 3+) 진입 시 `app-report` 가 이 모듈을 어떻게 소비할지는 별도 결정 (§5.3).

## 책임이 아닌 것

- 보정·쟁점추출 트리거 주기 결정 → `app-debate` 의 `UtteranceCorrector` / `IssueTreeExtractor` wrapper.
- 비즈니스 레벨 재시도 (보정 결과 신뢰도 낮으면 다른 프롬프트로 재호출) → 도메인 앱.
- 세션 단위 per-tenant quota → 도메인 앱 (벤더 quota 층과 분리).
- STT 호출 → `infra-stt`.

## 절대 깨면 안 되는 규칙 (§2.4)

1. **벤더 SDK 타입을 public 시그니처에 노출 금지.** `OpenAIResponse`, `com.anthropic.*`, `com.google.cloud.ai.*` 모두 인터페이스에서 보이면 안 됨. (향후 ArchUnit 규칙 §6.1).
2. `infra-stt` 에 의존 금지 (`infra-stt ↔ infra-llm` 무의존).
3. `app-*` 에 의존 금지.

## 인터페이스 결정 포인트 (§5.2, §6.1)

F1 PR 에서 결정. 임의로 정하지 말고 사용자에게 묻는다:

- **`LlmClient.complete()` 는 async 반환.** `CompletableFuture<LlmResponse>` 또는 Reactor 시그니처. sync API 만 노출하면 보정 latency 가 WebSocket broadcast 를 블로킹해 실시간 화면이 멈춘다.
- **`RequestContext(sessionId, useCase)`** 파라미터 — STT WER 을 세션별로, LLM 토큰 비용을 useCase 별(보정 vs 쟁점추출)로 슬라이스. 벤더 free 한 thin struct.
- **멀티 use case 빈 동시 활성화.** F1 은 보정용·쟁점추출용 두 모델을 **동시에** 사용. 단일 `@ConditionalOnProperty` 로 한 빈만 켜는 패턴 부적합 → `LlmAutoConfiguration` 이 `@Qualifier("corrector")` / `@Qualifier("issueExtractor")` 로 다중 빈 동시 노출.
- 메트릭 라벨 표준: `sessionId` / `useCase` / `vendor` / `model` / `status` 5차원 (§6.1).

## 빈 노출 방식 (§2.4)

- Spring `@Configuration` + `@Bean` auto-configuration. `app-*` 가 `@Import` 또는 `@ConditionalOnProperty` 로 활성화.
- `auto-configuration` 패키지 명명 규칙은 §6.1 미정 — 임시 후보: `com.debatetracker.infra.llm.config`.
- mock profile 1차부터 제공: `@ConditionalOnProperty(name="llm.mode", havingValue="mock")` → fixture 응답 빈. 보정용/쟁점추출용 각각 노출.
- 테스트에서 `@TestConfiguration` + `@Primary` mock 으로 덮어쓰는 패턴과 병행.

## 의존성

`infra-llm/build.gradle`:

```gradle
plugins { id 'java-library' }
```

OpenAI / Anthropic / Google SDK 등은 F1 구현 PR 에서 추가. 단 **public API 시그니처에서는 보이지 않아야** 한다 (§2.4).

## 보류 항목 (§5.3) — 1차에서 하지 않는다

- 벤더 무중단 런타임 전환. `@ConditionalOnProperty` 기반 재시작 시 전환만. 두 벤더 SDK 동시 classpath 상주는 `QAS-CO-02` 측정 기준이 확정된 뒤 재검토.
- `infra-common` 추출. retry/circuit breaker 가 `infra-stt` 와 비슷해 보여도 각자 구현 (§1).
- F2/F3 책임. 본 모듈의 F1 범위 인터페이스가 사후 분석/산출물에 충분한지는 Sprint 3+ 에서 별도 결정.

## NFR 책임 매핑

| NFR | 측정 대상 | 이 모듈에서 보장 |
|-----|---------|----------------|
| [PE-05](../docs/NFR.md) 세션 1회당 토큰 / 월 비용 상한 | 비용 통제 | 호출당 prompt/completion 토큰 기록, 벤더 usage API 풀링 |
| [FS-04](../docs/NFR.md) 보정 BERT Score F1 | 보정 품질 | 보정용 모델 호출 자체 (평가셋 기반 측정은 별도) |
| [FS-05](../docs/NFR.md) 쟁점 F1 ≥ 0.7 | 쟁점 추출 품질 | 쟁점추출용 모델 호출 자체 |
| [FS-06](../docs/NFR.md) 쟁점-근거 매핑 P/R ≥ 0.7 | 매핑 품질 | 쟁점추출 결과 |

## 테스트

- `LlmClient` 인터페이스 계약 테스트 (mock 어댑터 vs 실 어댑터 동일 동작).
- 벤더 어댑터별 통합 테스트는 실 API 호출 비용 부담 → 별도 프로파일 / 수동 트리거.
- LLM 프롬프트 회귀 평가는 NFR [MA-01](../docs/NFR.md) — 평가 데이터셋 + LLM-as-judge 로 CI 회귀 차단. 이 모듈이 직접 소유할지, 별도 평가 모듈로 분리할지는 미정.
- 인터페이스 격리 검증 — 어댑터 외부에서 벤더 specific 타입 import 0건 (향후 ArchUnit, §6.1).
