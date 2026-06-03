# common — CLAUDE.md

> 루트 [CLAUDE.md](../CLAUDE.md) 와 [docs/multi-module-strategy.md](../docs/multi-module-strategy.md) 를 먼저 읽고 와야 한다.

## 이 모듈의 본질

**현재는 공통 에러 객체만 관리한다.**

- `DebateTrackerException` (`com.debatetracker.exception`)
- `ErrorCode` enum (`com.debatetracker.exception`)

그 외 어떤 코드도 넣지 않는다.

`java-library` plugin. 4개 모듈 (`app-debate`, `app-report`, `infra-stt`, `infra-llm`) 이 implementation 으로 끌어다 쓴다.

## 절대 깨면 안 되는 규칙

1. **도메인 식별자/VO 금지** — `SessionId`, `Utterance`, `SpeakerId`, `IssueId` 같은 도메인 타입은 본 모듈에 넣지 않는다. 루트 CLAUDE.md §1 의 "중복 코드는 적극 수용한다" 가 우선. `common-domain` 도입 트리거 (식별자 3종 + VO 5종 또는 동일 enum 2회 이상 누적) 가 충족되기 전에는 각 앱이 각자 보유.
2. **비즈니스 로직 금지** — Service, Repository, Entity 어느 것도 안 됨.
3. **벤더 SDK 의존 금지** — Spring Web 포함. 현재 `ErrorCode.statusCode` 가 `int` 인 것도 이 이유. `infra-stt`/`infra-llm` 의 "벤더 중립 라이브러리" 콘셉트를 본 모듈이 깨면 안 됨.
4. **다른 모듈에 의존 금지** — 본 모듈은 의존성 그래프의 terminal node. `app-*`, `infra-*` 어디에도 의존하지 않는다.

## 책임 범위 — 들어가도 되는 것 / 들어가면 안 되는 것

| ✅ 들어가도 됨 | ❌ 들어가면 안 됨 |
|---|---|
| 예외 base class (`DebateTrackerException`) | 도메인 식별자 (`SessionId`, `Utterance` 등) |
| Error code enum (`ErrorCode`) | 도메인 enum (찬/반, 발화 상태 등) |
| (향후 필요 시) 공통 응답 envelope record | 비즈니스 로직, Service, Repository, Entity |
| (향후 필요 시) 진짜 횡단 유틸 (Spring 무의존) | 벤더 SDK wrapper (→ `infra-*` 책임) |
| | 트랜잭션·DB 관련 코드 (→ `app-*` 책임) |

새 코드를 본 모듈에 추가하기 전에 위 표에 부합하는지 PR 본문에 한 줄로 명시한다.

## 의존성

`common/build.gradle`:

```gradle
plugins {
    id 'java-library'
}
```

루트 `subprojects` 블록이 Java 21 toolchain, Lombok, JUnit 을 자동 주입. 별도 의존성 없음.

`api` 키워드 사용 금지 (현재). 만약 `infra-*` 의 public 인터페이스 시그니처에 `ErrorCode` / `DebateTrackerException` 이 노출되는 시점이 오면 그때 `api` 승격 검토.

## 코드 배치

```
common/src/main/java/com/debatetracker/exception/
├── DebateTrackerException.java
└── ErrorCode.java
```

- 패키지 prefix `com.debatetracker.exception` (모듈명 `common` 을 패키지에 박지 않음 — 의도적).
- 현재는 평탄 구조. ErrorCode 변종 (`ServerErrorCode` / `ClientErrorCode` 분리 등) 이나 Exception 변종이 필요해지는 시점에 `errorcode/` · `custom/` 등 서브패키지 분기 검토.

## 테스트

`DebateTrackerException` 과 `ErrorCode` 는 단순 데이터 wrapper / enum. 컨벤션 ([docs/convention.md](../docs/convention.md) — "단순 전달 메서드는 테스트하지 않는다") 에 따라 본 모듈은 테스트 코드 없음.

행동이 들어간 코드가 추가될 때 테스트 작성.

## 부트 실행

`java-library` 라 부트 불가. 다른 앱이 끌어다 쓰는 라이브러리.

```powershell
.\gradlew :common:build         # jar 산출
.\gradlew :common:compileJava   # 빠른 컴파일 검증
```
