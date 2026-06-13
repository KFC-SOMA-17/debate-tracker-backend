# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 한 줄

**디베이트 트래커(Debate Tracker)** 의 백엔드. 토론 음성을 실시간 STT → LLM 보정 → 쟁점 트리 → 사후 분석/산출물(세특·카드뉴스)까지 잇는 SaaS.

현재 단계: **Sprint 1-2 (F1 실시간 토론 요약)**. `app-debate` + `infra-stt` + `infra-llm` 이 1차 작업 범위이며 `app-report` 는 스켈레톤만 유지된다.

## 멀티 모듈 아키텍처

```
app-debate ──┐                                  app-report ──┐
             ├──► infra-stt                                  │
             ├──► infra-llm                                  ├──► infra-llm
             └──► common                                     ├──► common
                                                             │   (Sprint 3+)

infra-stt ──► common
infra-llm ──► common
```

| 모듈 | 종류 | 책임 |
|------|------|------|
| `app-debate` | Bootable Spring Boot (Sprint 1-2 본격 개발) | 세션 lifecycle, WebSocket broadcast, 발화/쟁점 도메인, 화자·찬반 라벨링. **DB 스키마 owner** |
| `app-report` | Bootable Spring Boot (Sprint 3+ 스켈레톤) | 사후 분석 F2, 카드뉴스/세특 F3. 1차에서 **DB read-only** |
| `infra-stt` | `java-library` | STT 외부 호출 어댑터. **벤더 중립 `SttClient` 인터페이스** + 구현체. Spring Events 로 전사 이벤트 발행 |
| `infra-llm` | `java-library` | LLM 외부 호출 어댑터. **벤더 중립 `LlmClient` 인터페이스** + 구현체. 보정용/쟁점추출용 빈 동시 노출 |
| `common` | `java-library` | **공통 에러 객체 + Spring/벤더 무의존 유틸.** 도메인 식별자/VO·비즈니스 로직·벤더 SDK 의존 금지. 자세한 규칙은 [common/CLAUDE.md](common/CLAUDE.md) |

### 절대 깨면 안 되는 규칙

1. `infra-*` 의 public 인터페이스는 **벤더 중립**. `OpenAIResponse`, `software.amazon.awssdk.*`, `Anthropic*` 같은 SDK·HTTP specific 타입을 시그니처에 노출하지 않는다.
2. **`app-* → app-*` 의존 금지.** 두 앱은 코드 의존이 없다 (DB 만 공유).
3. **`infra-* → app-*` 의존 금지.**
4. `infra-stt ↔ infra-llm` **무의존**.
5. 도메인 코드(`app-*`)는 벤더 SDK 타입을 직접 보지 않는다. 필요하면 앱 내부에 **어댑터 클래스** 를 둔다 — 예: `LlmUtteranceCorrectorAdapter`, `LlmDebateAgendaAnalyzerAdapter` 가 `LlmClient` 를 감싼다.

위 규칙은 향후 ArchUnit 으로 강제 예정. 신규 클래스를 추가할 때 import 만으로 위반 가능하니 주의.

### 중복 코드는 적극 수용한다

도메인 식별자·VO 가 두 앱에서 각자 정의되어도 1차에서는 OK. `common-domain` 모듈을 임의로 만들지 말 것. 중복이 과도하게 누적되면 그때 공통 모듈 신설 검토.

### F1 인터페이스 핵심 원칙

`infra-*` 인터페이스 변경 시 다음 원칙 준수:

- **`SttClient`**: 단발 호출이 아닌 스트리밍 방식. 벤더 transport 차이(Azure WebSocket 등)는 노출 금지. 전사 결과는 **Spring Events** 로 `app-*` 에 전달 (`@EventListener` 로 수신).
- **`LlmClient`**: 현재 동기 방식 (`refine()`, `extract()` 동기 반환). 향후 성능 요구사항 변경 시 비동기 전환 가능성 있음.
- **멀티 모델 지원**: 보정용·쟁점추출용 서로 다른 모델 사용. 현재는 Google Gemini (2.5-flash/pro) 단일 벤더. 향후 멀티 벤더 확장 가능.
- **테스트 격리**: `@ConditionalOnProperty(name="llm.mode" / "stt.mode")` 기반 mock 빈 제공. 실 API 호출 비용 격리.

## 현재 벤더 선택 (1차)

- **STT**: Azure Speech (Realtime WebSocket) — `infra-stt` Azure 어댑터
- **LLM**: Google Gemini — `spring.ai.google.genai`
  - 보정용: `gemini-2.5-flash` (`llm.refine.model`)
  - 쟁점추출용: `gemini-2.5-pro` (`llm.extract.model`)
- **DB**: MySQL (로컬 Docker `localhost:13306`, 테스트는 프로파일 분기)
- **캐시**: Redis (로컬 `localhost:16379`)

향후 멀티 벤더 확장 가능 (AWS Transcribe, OpenAI, Anthropic 등). 1차는 단일 벤더로 검증.

## 빌드 / 테스트 명령

Gradle wrapper 사용 (Windows PowerShell — `./gradlew` 대신 `.\gradlew`).

```powershell
# 전체 빌드 + 테스트
.\gradlew build

# 컴파일만 (빠른 확인)
.\gradlew compileJava

# 특정 모듈만
.\gradlew :app-debate:build
.\gradlew :infra-llm:test

# 단일 테스트 클래스
.\gradlew :app-debate:test --tests "com.debatetracker.debate.DebateApplicationTests"

# 단일 테스트 메서드
.\gradlew :app-debate:test --tests "com.debatetracker.debate.SomeTest.someMethod"

# 부트 실행 (app-* 만 가능)
.\gradlew :app-debate:bootRun
.\gradlew :app-report:bootRun

# fat-jar 산출
.\gradlew :app-debate:bootJar     # → app-debate/build/libs/app-debate-0.0.1-SNAPSHOT.jar
.\gradlew :app-report:bootJar
```

- Java 21 toolchain (`build.gradle` 의 `subprojects { java { toolchain ... } }`).
- Spring Boot 3.5.14, BOM 으로 버전 관리. 개별 의존성에 버전 박지 말 것.
- Lombok 은 모든 모듈에 자동 적용. 단 entity 에서는 `@RequiredArgsConstructor` 금지.
- `app-*` 에만 `org.springframework.boot` plugin 활성, `infra-*` 는 `java-library`. fat-jar 는 `app-*` 두 개만 산출.

## 설정 파일 구조

`app-debate/src/main/resources/`:
- `application.yml` — 공통 설정, profiles.active
- `application-local.yml` — 로컬 환경 (MySQL, Redis 접속 정보, CORS)
- `stt-config.yml` — Azure Speech 설정, 오디오 포맷
- `llm-config.yml` — Google Gemini 설정, 모델 선택

환경 변수 주입:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `AZURE_SUBSCRIPTION_KEY`, `AZURE_REGION`, `STT_AZURE_ENABLED`
- `GOOGLE_API_KEY`

## 코드 컨벤션

한국어 우아한형제들 스타일 베이스. 핵심 규칙:

- **Final 키워드** 메서드 파라미터/로컬 변수/클래스에 금지 (우아한형제들 가이드 따름 — 일반적인 Java 권장과 반대 방향이니 주의).
- **DTO 는 `record`**. 요청은 `xxxRequest` (저장은 `xxxSaveRequest`), 응답은 `xxxResponse`. List 응답은 객체로 한 번 더 감싸기 (`MembersResponse(List<MemberResponse> members)`).
- **정적 팩토리 메서드** 는 record DTO 에서만 허용. 일반 Entity/Domain 에서는 금지.
- **Entity** 는 `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. `@RequiredArgsConstructor` 는 Entity 에 금지.
- **금지 Lombok**: `@AllArgsConstructor`, `@Data`, `@Value`, `@EqualsAndHashCode`, `@Log`.
- **메서드 순서**: public 먼저 → 관련 private 바로 아래 → 공통 private 맨 아래. CRUD 순서 (Create → Read → Update → Delete).
- **어노테이션** 은 길이 순 정렬 (옵션 제외). 예: `@Entity` → `@NoArgsConstructor`.
- **메서드 체이닝** 한 줄에 한 점 (단 필드 접근/디미터 준수는 예외).
- **Static import** 비즈니스 코드 금지, 테스트 코드 허용.
- **검증 분리**: 입력 검증(형식·null)은 DTO, 비즈니스 정책 검증은 도메인.

## DB 공유와 마이그레이션

- 1차에서 두 앱이 **같은 MySQL** 을 본다.
- **`app-debate` 가 스키마 owner** — `app-report` 는 read-only 접속 (MySQL 계정 + repository 분리 권장).
- 스키마 변경 PR 은 **양쪽 앱 영향 확인 체크박스** 필수. `app-debate` 스키마가 바뀌면 `app-report` 의 read 엔티티/DTO 매핑도 같이 손봐야 한다.
- 메인 런타임은 **MySQL** (`runtimeOnly 'com.mysql:mysql-connector-j'`, 로컬은 Docker Compose). 테스트도 **MySQL** 로 일원화 — 로컬은 떠 있는 MySQL(`debate` DB)/Redis(포트 16379) 에 접속(`local` 프로파일, 기본값), CI 는 Testcontainers(`ci` 프로파일, `SPRING_PROFILES_ACTIVE=ci`). 테스트 DB 정리는 `DatabaseCleaner`(MySQL `TRUNCATE` 기반).

## 트래픽 분리 / 배포

- 두 앱은 **별도 JVM / 별도 ECS Task / 별도 Auto Scaling**. `app-debate` 는 실시간(WebSocket), `app-report` 는 배치성. SLO·스케일 정책이 독립.
- 한쪽 앱의 OOM/deadlock 이 다른 쪽에 영향 없음. 단 공유 DB 자원 경쟁은 잔존 리스크.
- GitHub Actions `paths` 필터로 변경 영향 분기 권장 — 한쪽 앱만 변경된 PR 은 다른 앱 재배포 스킵.

## 변경 시 잊지 말 것

1. PR 본문에 **"양쪽 앱 영향 확인"** (DB 스키마/공유 데이터 변경 시).
2. `infra-*` 의 인터페이스에 벤더 타입이 새어 나가지 않았는지 확인.
3. `app-debate` 에 새 use case 가 생기면 → `LlmClient` 직접 호출보다 **어댑터 클래스** 하나 끼우기 (`LlmUtteranceCorrectorAdapter`, `LlmDebateAgendaAnalyzerAdapter` 패턴).
4. Sprint 1-2 동안 `app-report` 본격 변경 금지. 빈 모듈 스켈레톤만 유지.

## 모듈별 안내

각 모듈 루트의 `CLAUDE.md` 도 같이 본다:

- [app-debate/CLAUDE.md](app-debate/CLAUDE.md)
- [app-report/CLAUDE.md](app-report/CLAUDE.md)
- [infra-stt/CLAUDE.md](infra-stt/CLAUDE.md)
- [infra-llm/CLAUDE.md](infra-llm/CLAUDE.md)
- [common/CLAUDE.md](common/CLAUDE.md)
