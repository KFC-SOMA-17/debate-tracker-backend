# common — CLAUDE.md

> 루트 [CLAUDE.md](../CLAUDE.md) 를 먼저 읽고 와야 한다.

## 이 모듈의 본질

**공통 에러 객체 + Spring/벤더 무의존 유틸.**

현재 포함:
- `exception/` — `DebateTrackerException`, `ErrorCode`
- `serdes/` — `JsonUtils` (Jackson 기반 직렬화/역직렬화)

`java-library` plugin. 4개 모듈 (`app-debate`, `app-report`, `infra-stt`, `infra-llm`) 이 implementation 으로 끌어다 쓴다.

## 절대 깨면 안 되는 규칙

1. **도메인 식별자/VO 금지** — `SessionId`, `Utterance`, `SpeakerId`, `IssueId` 같은 도메인 타입은 본 모듈에 넣지 않는다. 루트 CLAUDE.md 의 "중복 코드는 적극 수용한다" 원칙 우선. 중복이 과도하게 누적되면 그때 `common-domain` 신설 검토.
2. **비즈니스 로직 금지** — Service, Repository, Entity 어느 것도 안 됨.
3. **벤더 SDK 의존 금지** — Spring Web 포함. 현재 `ErrorCode.statusCode` 가 `int` 인 것도 이 이유. `infra-stt`/`infra-llm` 의 "벤더 중립 라이브러리" 콘셉트를 본 모듈이 깨면 안 됨.
4. **다른 모듈에 의존 금지** — 본 모듈은 의존성 그래프의 terminal node. `app-*`, `infra-*` 어디에도 의존하지 않는다.

## 책임 범위 — 들어가도 되는 것 / 들어가면 안 되는 것

| ✅ 들어가도 됨 | ❌ 들어가면 안 됨 |
|---|---|
| 예외 base class (`DebateTrackerException`) | 도메인 식별자 (`SessionId`, `Utterance` 등) |
| Error code enum (`ErrorCode`) | 도메인 enum (`Stance`, `EvidenceType` 등) |
| Spring/벤더 무의존 유틸 (`JsonUtils`) | 비즈니스 로직, Service, Repository, Entity |
| 공통 응답 envelope record (향후) | 벤더 SDK wrapper (→ `infra-*` 책임) |
| | 트랜잭션·DB 관련 코드 (→ `app-*` 책임) |
| | Spring Web, JPA, Redis 등 프레임워크 의존 코드 |

**추가 규칙**:
- 유틸 클래스는 **static 메서드만** (인스턴스 상태 금지).
- 외부 라이브러리 의존 시 **벤더 중립성 확인** — Jackson (OK), Spring Framework (NG), 벤더 SDK (NG).
- 새 코드 추가 전 PR 본문에 **"common 정책 부합 확인"** 한 줄 명시.

## 의존성

`common/build.gradle`:

```gradle
plugins {
    id 'java-library'
}

dependencies {
    implementation 'com.fasterxml.jackson.core:jackson-databind'  // JsonUtils 용
    implementation 'org.slf4j:slf4j-api'
    api 'io.micrometer:micrometer-core'  // infra-* 모듈에 전파
}
```

루트 `subprojects` 블록이 Java 21 toolchain, Lombok, JUnit 을 자동 주입.

**`api` vs `implementation`**: 벤더 중립 라이브러리(Micrometer, SLF4J)는 `api`로 노출 가능. `infra-*` 모듈들이 공통으로 사용하는 메트릭/로깅 추상화를 transitive dependency로 전파한다.

## 코드 배치

```
common/src/main/java/com/debatetracker/
├── exception/
│   ├── DebateTrackerException.java
│   └── ErrorCode.java
└── serdes/
    └── JsonUtils.java
```

- 패키지 prefix `com.debatetracker.*` (모듈명 `common` 을 패키지에 박지 않음 — 의도적).
- 기능별 서브패키지 분리 (`exception/`, `serdes/`).
- 향후 유틸 추가 시 `util/`, `time/` 등 추가 가능. 단 **Spring/벤더 무의존** 확인 필수.

## 테스트

- `DebateTrackerException`, `ErrorCode` — 단순 데이터 wrapper / enum. 테스트 불필요.
- `JsonUtils` — 직렬화/역직렬화 로직. 단위 테스트 작성 권장 (예외 처리, 타입 변환 검증).
- 컨벤션: 단순 전달 메서드는 테스트하지 않지만, **비즈니스 로직이 들어간 유틸**은 테스트 필수.

## 부트 실행

`java-library` 라 부트 불가. 다른 앱이 끌어다 쓰는 라이브러리.

```powershell
.\gradlew :common:build         # jar 산출
.\gradlew :common:compileJava   # 빠른 컴파일 검증
```
