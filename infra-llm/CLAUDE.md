# infra-llm — CLAUDE.md

> 루트 [CLAUDE.md](../CLAUDE.md) 를 먼저 읽고 와야 한다.

## 이 모듈의 본질

**벤더 중립 LLM 클라이언트 + 벤더별 어댑터.** OpenAI, Anthropic, Google 등 외부 LLM 서비스를 도메인 앱에 일관된 인터페이스로 노출한다.

`java-library` plugin. `app-debate` 와 `app-report` 양쪽이 implementation 으로 끌어다 쓴다.

**핵심 원칙**: 인터페이스 시그니처는 벤더 중립. 벤더 SDK 타입 노출 금지.

## 책임 범위

- **벤더 중립 인터페이스**: `LlmClient` — `refine()` (보정), `extract()` (쟁점추출). 현재 동기 방식, 향후 비동기 전환 가능.
- **다중 모델 지원**: 현재 AWS Bedrock (Haiku 4.5 보정용, Sonnet 5 쟁점추출용). 향후 멀티 벤더 확장 가능 (OpenAI, Google Gemini 등).
- **어댑터 구현**: `adapter/SpringAiLlmClient` — Spring AI 기반 AWS Bedrock Converse 연동.
- **자동 설정**: `config/LlmAutoConfiguration` — `llm.enabled=true` 시 빈 등록.
- **요청/응답 DTO**: 벤더 중립 타입 (`RefineRequest/Response`, `ExtractAgendaRequest/Response`). 도메인 객체는 `app-*` 가 관리.

F2/F3 (Sprint 3+) 진입 시 `app-report` 소비 방식은 별도 결정.

## 책임이 아닌 것

- 보정·쟁점추출 트리거 주기 결정 → `app-debate` 의 스케줄러 (`TranscribeRefiningScheduler`, `AgendaAnalyzeScheduler`).
- 도메인 객체 변환 → `app-debate` 의 어댑터 클래스 (`LlmUtteranceCorrectorAdapter`, `LlmDebateAgendaAnalyzerAdapter`).
- 비즈니스 레벨 재시도 → 도메인 앱.
- STT 호출 → `infra-stt`.

## 절대 깨면 안 되는 규칙

1. **벤더 SDK 타입을 public 시그니처에 노출 금지.** `software.amazon.awssdk.*`, `com.anthropic.*`, OpenAI 등 모두 인터페이스에서 보이면 안 됨. (향후 ArchUnit 규칙).
2. `infra-stt` 에 의존 금지 (`infra-stt ↔ infra-llm` 무의존).
3. `app-*` 에 의존 금지.

## 현재 인터페이스 정책

- **동기 방식**: `LlmClient.refine()`, `extract()` 모두 동기 반환. 향후 성능 요구사항 변경 시 비동기 전환 가능.
- **요청/응답 타입**: 벤더 중립 DTO (`RefineRequest/Response`, `ExtractAgendaRequest/Response`, `TranscriptSegment`, `ExtractAgenda/Claim/Evidence` 등).
- **빈 등록**: `LlmAutoConfiguration` — 단일 `LlmClient` 빈. 보정/쟁점추출은 동일 클라이언트 사용, 모델만 다름 (설정: `llm.refine.model`, `llm.extract.model`).

## 빈 노출 방식

- **자동 설정**: `config/LlmAutoConfiguration` — `@ConditionalOnProperty(name="llm.enabled", havingValue="true")` 기반.
- **실 구현**: Spring AI Bedrock Converse 기반 AWS Bedrock 연동.
- **Mock 구현**: `app-debate` 의 `test/fixture/FakeLlmClient` 사용. 테스트 격리용. `@ConditionalOnProperty(name="llm.mode", havingValue="mock")` 또는 `@MockBean` 사용.
- **설정 위치**: `llm-config.yml` — `spring.ai.bedrock.converse.*`, `llm.refine.model`, `llm.extract.model`.

## 의존성

`infra-llm/build.gradle`:

```gradle
plugins { id 'java-library' }

dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.ai:spring-ai-starter-model-bedrock-converse'
    // 향후 멀티 벤더 확장 시 OpenAI, Google GenAI 스타터 추가 가능
}
```

**중요**: 벤더 SDK 타입 (`software.amazon.awssdk.*` 등)을 **public API 시그니처에 노출 금지**.

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `AWS_ACCESS_KEY_ID` | IAM 사용자 액세스 키 | — |
| `AWS_SECRET_ACCESS_KEY` | IAM 사용자 시크릿 키 | — |
| `AWS_REGION` | Bedrock 사용 리전 | `us-east-1` |

ECS/EC2·로컬 모두 환경변수 방식 사용. AWS SDK `DefaultCredentialsChain` 이 자동으로 읽는다.

## 향후 확장 가능성

- **멀티 벤더**: 현재 AWS Bedrock 단일 벤더. 향후 OpenAI, Google Gemini 어댑터 추가 가능.
- **비동기 전환**: 현재 동기 방식. 성능 요구사항 변경 시 `CompletableFuture` / Reactor 기반 비동기 전환 가능.
- **retry/circuit breaker**: 현재 미구현. 필요 시 벤더별 에러 핸들링 추가.
- **`infra-common` 추출**: retry/circuit breaker 가 `infra-stt` 와 중복되면 공통 모듈 검토. 1차에서는 각자 구현.

## 테스트

- **인터페이스 계약 테스트**: `LlmClient` 의 `refine()`, `extract()` 동작 검증.
- **실 API 테스트**: 실 AWS Bedrock 호출 비용 부담 → 별도 프로파일 / 수동 트리거. 환경변수 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` 설정 후 `@Disabled` 제거해 실행.
- **Mock 테스트**: `app-debate` 의 `FakeLlmClient` 사용. 로컬 개발·CI 에서 실 API 호출 차단.
- **인터페이스 격리 검증**: 어댑터 외부에서 벤더 SDK 타입 (`software.amazon.awssdk.*` 등) import 금지. 향후 ArchUnit 으로 강제 예정.
