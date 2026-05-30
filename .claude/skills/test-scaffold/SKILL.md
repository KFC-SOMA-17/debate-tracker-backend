---
name: test-scaffold
description: debate-tracker-backend 팀 컨벤션에 맞춰 Java/Spring 테스트 코드를 작성한다. Nested 클래스 + 한글 메서드명 + Base 클래스 상속 + Generator 기반 fixture 패턴. REST API 구현 시 Controller 테스트와 RestDocs Document 테스트를 함께 작성한다.
---

# test-scaffold

지정한 소스 클래스/메서드에 대해 팀 테스트 컨벤션(`.claude/conventions/code-convention.md` §7)에 맞는 테스트를 생성한다.

## 입력

- **target**: 테스트할 소스 클래스 또는 메서드 (예: `DebateService`, `DebateRestController#createDebate`).
- **type** (선택): `controller` | `document` | `service` | `persistence` | `jpa-repository` | `domain`. 생략 시 target 클래스의 패키지로 자동 판별.

## 사전 작업

### 1. 모듈 식별

target 클래스의 경로로 모듈을 식별:

| 경로 prefix | 모듈 |
|---|---|
| `app-debate/src/main/java/com/debatetracker/debate/**` | `app-debate` |
| `app-report/src/main/java/com/debatetracker/report/**` | `app-report` |
| `infra-stt/src/main/java/com/debatetracker/infra/stt/**` | `infra-stt` |
| `infra-llm/src/main/java/com/debatetracker/infra/llm/**` | `infra-llm` |

패키지 베이스는 `com.debatetracker.{module}`. 모듈 구조·의존성 규칙의 최신 기준은 루트 `CLAUDE.md` 를 본다.

### 2. 모듈별 가능한 레이어

| 모듈 | 가능한 레이어 |
|---|---|
| `app-debate` | controller, document, service, persistence(infrastructure), jpa-repository, domain |
| `app-report` | controller, document, service, persistence(infrastructure), jpa-repository, domain |
| `infra-stt` | (client adapter) domain 단위 테스트, mock profile 통합 테스트 |
| `infra-llm` | (client adapter) domain 단위 테스트, mock profile 통합 테스트 |

`infra-*`는 controller/repository가 없음. WebSocket/REST는 `app-*`에만 존재.

### 3. 테스트 파일 위치

소스 패키지 구조를 미러링:

```
app-debate/src/main/java/com/debatetracker/debate/service/debate/DebateService.java
  → app-debate/src/test/java/com/debatetracker/debate/service/debate/DebateServiceTest.java

app-debate/src/main/java/com/debatetracker/debate/controller/debate/DebateRestController.java
  → app-debate/src/test/java/com/debatetracker/debate/controller/debate/DebateRestControllerTest.java   (행위 검증)
  → app-debate/src/test/java/com/debatetracker/debate/document/debate/DebateDocumentTest.java            (API 문서화)
```

Controller 는 **테스트 파일이 두 곳**에 생긴다 — `controller.*` (행위) 와 `document.*` (RestDocs 문서). 아래 §"REST API 구현 시 필수 규칙" 참조.

### 4. 소스 우선 읽기

테스트 작성 전 **반드시 target 클래스의 모든 public 메서드, 파라미터, 반환 타입, 던질 수 있는 예외를 파악**한다. private helper에 분기가 있다면 그 분기도 호출 메서드의 edge case로 묶어 테스트 대상.

Controller 의 경우 추가로: HTTP 메서드/경로, `@RequestBody` DTO 필드, 응답 DTO 필드, `@Valid` 유무, 그리고 서비스가 던지는 예외가 `GlobalExceptionHandler` 를 거쳐 어떤 status/error code 로 매핑되는지 파악한다. 문서화할 success/error 케이스를 여기서 결정한다.

## 레이어별 Base 클래스 & 어노테이션

실제 프로젝트(`app-debate`) 기준:

| 레이어 | 패키지 | Base 클래스 | WebEnvironment | 핵심 어노테이션 | 하위 협력자 |
|---|---|---|---|---|---|
| Controller | `{module}.controller.*` | `BaseControllerTest` (public) | `RANDOM_PORT` | `@ExtendWith(DatabaseCleaner.class)`, `@SpringBootTest` | **mock 없음 — 실제 빈 전부 기동** |
| Document | `{module}.document.*` | `BaseDocumentTest` (public) | `RANDOM_PORT` | `@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})`, `@SpringBootTest` | **서비스를 `@MockitoBean` 으로 mock** |
| Service | `{module}.service.*` | `BaseServiceTest` (public) | `NONE` | `@ExtendWith(DatabaseCleaner.class)`, `@SpringBootTest` | 실제 repository (mock 금지) |
| Persistence (DomainRepository 어댑터) | `{module}.infrastructure.persistence.*` | `BaseDomainRepositoryTest` (public) | `NONE` | `@ExtendWith(DatabaseCleaner.class)`, `@SpringBootTest` | 실제 JPA repository |
| Jpa Repository | `{module}.infrastructure.persistence.jpa.*` | `BaseJpaRepositoryTest` (package-private) | N/A | `@DataJpaTest` | slice — JPA 빈만 |
| Domain | `{module}.domain.*` | 없음 (plain JUnit) | N/A | N/A | N/A |

- **DB 격리는 `DatabaseCleaner`** (`org.junit.jupiter.api.extension.BeforeEachCallback` 구현체). 매 테스트 전 truncate. `@Transactional` 로 격리하지 않는다.
- `BaseControllerTest` / `BaseServiceTest` / `BaseDomainRepositoryTest` / `BaseDocumentTest` 는 `public`. 따라서 하위 패키지(`controller.debate`, `document.debate` 등)에서 상속 가능하다. **새 Base 클래스를 package-private 으로 만들지 말 것** — 하위 패키지 상속이 깨진다.

**Base 클래스가 아직 프로젝트에 없으면**:
- target 파일을 작성하되 상단 주석으로 `// TODO ...` 를 남기지 말고, **사용자에게 알리고 진행 여부를 묻는다**. 베이스 클래스 도입은 인프라 결정이므로 임의 생성 금지.

## REST API 구현 시 필수 규칙 — Controller 테스트 + Document 테스트는 한 쌍

> **Controller endpoint 를 구현/수정하면 항상 두 개의 테스트를 함께 추가/갱신한다.**

| 테스트 | 위치 | 목적 | 하위 협력자 |
|---|---|---|---|
| **Controller 테스트** (`{Domain}RestControllerTest`) | `controller.{domain}` | endpoint 의 **실제 동작**을 end-to-end 로 검증. 상태코드 + 응답 본문 값. | mock 없이 실제 빈(service, repository, H2) 전부 사용 |
| **Document 테스트** (`{Domain}DocumentTest`) | `document.{domain}` | **REST Docs/OpenAPI 스니펫 생성**. 요청/응답 필드·헤더·쿠키 스펙을 문서화. | service 를 `@MockitoBean` 으로 mock (DB 미접근, 문서화에 집중) |

둘은 책임이 다르므로 **둘 다** 필요하다:

- Controller 테스트가 통과해도 Document 테스트가 없으면 API 문서가 생성되지 않는다.
- Document 테스트는 service 를 mock 하므로 비즈니스 동작을 보장하지 않는다 — 그건 Controller/Service 테스트의 몫.

새 endpoint PR 의 완료 정의(DoD): **(1) Controller 테스트 추가, (2) Document 테스트 추가, (3) 두 테스트 모두 통과, (4) `build/generated-snippets/{prefix}/{status}/` 에 스니펫 생성 확인.**

Document 테스트에서 mock 할 service 가 `BaseDocumentTest` 에 아직 선언되어 있지 않으면 `@MockitoBean` 필드를 추가한다(공유라면 Base 에, 단발성이면 해당 테스트 클래스에). 새 API 그룹이면 `Tag` enum 에 항목을 추가한다.

## 테스트 클래스 구조

```java
class {ClassName}Test extends {BaseClass} {

    @Autowired
    private {TargetClass} {targetField};  // Controller(행위)/Service/Persistence — 실제 빈 주입

    @Nested
    class {MethodName} {     // public 메서드 / endpoint 당 @Nested 1개

        @Test
        void 한글로_테스트_의도를_설명한다() {
            // Arrange (데이터 준비)

            // Act (테스트 대상 실행)

            // Assert (AssertJ/JUnit 검증)
        }
    }
}
```

**중요:**
- `given-when-then` 주석 **금지**, 대신 빈 줄로 단계를 구분 (팀 컨벤션 `.claude/conventions/code-convention.md` §7).
- `final` 키워드는 **메서드 파라미터/로컬 변수/클래스**에 사용 금지. (단, 인스턴스 필드의 `final` 과 상수 `static final` 은 허용 — Document 테스트의 `private final RestDocsRequest requestDocument` 가 그 예.)
- 테스트 클래스/메서드에 `@Transactional` 붙이지 않음 (`DatabaseCleaner` 가 격리 담당).

## 메서드 네이밍

한글 + 언더스코어. 패턴:

- 성공: `{기능}을_할_수_있다`, `{결과}를_반환한다`, `{기능}_성공`
- 예외: `{조건}이면_예외가_발생한다`, `존재하지_않는_{객체}이면_예외를_던진다`, `{조건}_실패`
- 필터링: `{조건}으로_필터링하여_조회할_수_있다`

예시 (실제/예정 도메인):
- `토론을_생성하면_식별자가_부여된_토론을_반환한다`
- `저장한_토론이_실제로_영속화된다`
- `존재하지_않는_세션이면_예외를_던진다`

## Fixture / Generator (현재 상태 주의)

- **Generator 는 아직 도입되지 않았다.** `app-debate/src/test/.../fixture/` 에는 `package-info.java` 만 있고(향후 `@Component` + `generate(...)` 패턴 예정), 구현체는 없다.
- 따라서 **현재 통합 테스트(Service/Persistence)에서도 도메인 객체를 `new` 로 직접 생성**한다. 예: `new Debate(null, "토론 주제")`. 실제 코드(`DebateServiceTest`, `DebateDomainRepositoryTest`)가 이 방식이다.
- **Generator 를 임의로 만들지 말 것.** fixture 도입은 모듈/도메인 합의 사항이다. 필요하다고 판단되면 사용자에게 알린다.

**Generator 도입 후(향후) 규칙** — 도입되면 그때부터 적용:

| Generator (예정) | 용도 |
|---|---|
| `sessionGenerator.generate(...)` | 토론 세션 |
| `utteranceGenerator.generate(...)` | 발화 |
| `speakerGenerator.generate(...)` | 화자 |
| `issueGenerator.generate(...)` | 쟁점 |

- 모든 generator 는 base test class 에서 `protected` 필드로 노출.
- 통합 테스트에서는 `new` 대신 generator 사용, domain 단위 테스트에서는 `new` 허용.

## Controller 테스트 패턴 (RestAssured, 실제 빈)

`BaseControllerTest` 의 `given()` (인자 없음) 으로 시작. mock 없이 실제 동작을 검증한다.

```java
class DebateRestControllerTest extends BaseControllerTest {

    @Nested
    class CreateDebate {

        @Test
        void 토론을_생성한다() {
            DebateCreateRequest request = new DebateCreateRequest("인공지능은 인간의 일자리를 대체할 수 있는가");

            DebateCreateResponse response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/debates")
                    .then()
                    .statusCode(200)
                    .extract().as(DebateCreateResponse.class);

            assertAll(
                    () -> assertThat(response.debateId()).isNotNull(),
                    () -> assertThat(response.topic()).isEqualTo("인공지능은 인간의 일자리를 대체할 수 있는가")
            );
        }
    }
}
```

- 인증/인가 헬퍼(`accessToken(...)`)는 **아직 없다.** 인증이 도입되면 그때 401/403 케이스를 추가한다 — 지금 임의로 작성 금지.
- 경로 변수는 `.pathParam("id", value)` + `.post("/api/.../{id}")`.

## Document 테스트 패턴 (RestDocs, service mock)

`document.{domain}` 패키지, `public class {Domain}DocumentTest extends BaseDocumentTest`. 빌더 헬퍼는 `BaseDocumentTest` 가 `protected` 로 제공: `request()`, `response()`, `document(prefix, statusCode)`, `given(filter)`, 상수 `ERROR_RESPONSE`.

### 빌더 API

- `request()` → `RestDocsRequest`: `.tag(Tag.X)`, `.summary(...)`, `.description(...)`, `.requestBodyField(...)`, `.pathParameter(...)`, `.queryParameter(...)`, `.requestHeader(...)`, `.requestCookie(...)`, `.multipartField(...)`
- `response()` → `RestDocsResponse`: `.responseBodyField(...)`, `.responseHeader(...)`, `.responseCookie(...)`
- 필드 디스크립터: `fieldWithPath("topic").type(STRING).description("토론 주제")` — 배열은 `fieldWithPath("items[].id").type(NUMBER)...`
- `document("debate/create", 200).request(requestDocument).response(responseDocument).build()` → `RestDocumentationFilter`. 스니펫은 `build/generated-snippets/debate/create/200/` 에 생성된다.
- `Tag` enum 에 API 그룹별 항목 추가 (예: `DEBATE_API("Debate API")`).

### 성공 케이스 (service mock)

```java
public class DebateDocumentTest extends BaseDocumentTest {

    @Nested
    class CreateDebate {

        private final RestDocsRequest requestDocument = request()
                .tag(Tag.DEBATE_API)
                .summary("토론 생성")
                .requestBodyField(
                        fieldWithPath("topic").type(STRING).description("토론 주제")
                );

        private final RestDocsResponse responseDocument = response()
                .responseBodyField(
                        fieldWithPath("debateId").type(STRING).description("토론 ID"),
                        fieldWithPath("topic").type(STRING).description("토론 주제")
                );

        @Test
        void 토론_생성_성공() {
            DebateCreateRequest request = new DebateCreateRequest("인공지능은 인간의 일자리를 대체할 수 있는가");
            Debate debate = new Debate(1L, "인공지능은 인간의 일자리를 대체할 수 있는가");
            doReturn(debate).when(debateService).create(any(Debate.class));

            RestDocumentationFilter document = document("debate/create", 200)
                    .request(requestDocument)
                    .response(responseDocument)
                    .build();

            given(document)
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when().post("/api/debates")
                    .then().statusCode(200);
        }
    }
}
```

- mock 할 service 는 `BaseDocumentTest` 의 `@MockitoBean` 필드(`debateService` 등). 없으면 추가한다.
- 도메인 객체는 `equals` 를 재정의하지 않으므로 stub 인자는 **`any(...)` 매처**로 매칭한다 (`any(Debate.class)`).

### 실패(에러) 케이스 — **항상 `@ParameterizedTest` + `@EnumSource`**

> **실패 케이스는 코드가 1개여도 무조건 `@ParameterizedTest` + `@EnumSource(value = ErrorCode.class, names = {...})` 로 작성한다.** 가독성·통일성 + 코드 추가 시 `names` 한 줄만 늘리면 되는 확장성 때문. 단발 `@Test` 로 풀어쓰지 말 것.

- 에러 코드는 `common` 모듈의 `com.debatetracker.exception.ErrorCode` enum. **현재는 도메인별 코드가 없고 인프라 레벨 코드만 존재** (`FIELD_ERROR` 400, `NO_RESOURCE_FOUND` 404, `INTERNAL_SERVER_ERROR` 500 등). status 는 `errorCode.getStatusCode()` 로 꺼내 `.then().statusCode(...)` 와 `document(prefix, ...)` 양쪽에 쓴다.
- 본문 스펙은 공용 상수 `ERROR_RESPONSE` (`code`/`status`/`message`). `document("debate/create", errorCode.getStatusCode())` (errorCode 오버로드는 이 프로젝트에 없으므로 int). **같은 status 의 코드를 여러 개 문서화하면 스니펫 경로(`.../{status}/`)가 충돌**하므로 그때는 prefix 를 코드별로 다르게 준다.

에러는 두 갈래로 발생한다:

**(1) 요청 검증 실패 (`@Valid` → 400 `FIELD_ERROR`)** — service 호출 **전** binding 단계에서 실패. service stub 불필요.

> ⚠️ **요청 본문 검증 에러는 본문 필드 스니펫을 문서화하지 않는다.** `requestBodyField(...)` 로 `topic` 을 STRING 으로 문서화한 상태에서 검증 실패용 요청(`topic=null` 또는 누락)을 보내면 RestDocs 가 `FieldTypesDoNotMatchException: documented type ... is String but the actual type is Null` 을 던진다. 그래서 **에러 케이스 전용으로 본문 필드 없는 요청 문서(`errorRequestDocument` = tag + summary 만)** 를 따로 만들어 쓴다. 요청 본문 계약은 성공 케이스 스니펫이 책임진다.

```java
        private final RestDocsRequest errorRequestDocument = request()
                .tag(Tag.DEBATE_API)
                .summary("토론 생성");

        @EnumSource(value = ErrorCode.class, names = {"FIELD_ERROR"})
        @ParameterizedTest
        void 토론_주제가_없으면_생성에_실패한다(ErrorCode errorCode) {
            DebateCreateRequest request = new DebateCreateRequest(null);

            RestDocumentationFilter document = document("debate/create", errorCode.getStatusCode())
                    .request(errorRequestDocument)
                    .response(ERROR_RESPONSE)
                    .build();

            given(document)
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when().post("/api/debates")
                    .then().statusCode(errorCode.getStatusCode());
        }
```

**(2) service 가 던지는 비즈니스 예외 (`DebateTrackerException`)** — `GlobalExceptionHandler` 가 status/body 로 매핑. service 를 `doThrow` 로 stub. 유효한 본문을 보내므로 `requestDocument`(본문 필드 포함) 를 그대로 써도 된다.

```java
        @EnumSource(value = ErrorCode.class, names = {"NO_RESOURCE_FOUND"})
        @ParameterizedTest
        void 존재하지_않으면_실패한다(ErrorCode errorCode) {
            doThrow(new DebateTrackerException(errorCode)).when(debateService).someMethod(any());

            RestDocumentationFilter document = document("debate/get", errorCode.getStatusCode())
                    .request(requestDocument)
                    .response(ERROR_RESPONSE)
                    .build();

            given(document)
                    .when().get("/api/debates/{id}", 999L)
                    .then().statusCode(errorCode.getStatusCode());
        }
```

- **실제로 발생 가능한 에러만 문서화한다.** 검증·도메인 예외가 없는 endpoint 면 성공 케이스만. 없는 실패를 억지로 지어내지 말 것.

## Service 테스트 패턴 (실제 repository)

```java
class DebateServiceTest extends BaseServiceTest {

    @Autowired
    private DebateService debateService;

    @Nested
    class Create {

        @Test
        void 토론을_생성하면_식별자가_부여된_토론을_반환한다() {
            Debate debate = new Debate(null, "인공지능은 인간의 일자리를 대체할 수 있는가");

            Debate actual = debateService.create(debate);

            assertAll(
                    () -> assertThat(actual.getId()).isNotNull(),
                    () -> assertThat(actual.getTopic()).isEqualTo("인공지능은 인간의 일자리를 대체할 수 있는가")
            );
        }

        @Test
        void 존재하지_않으면_예외를_던진다() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> debateService.someMethod(invalidArgs));

            assertThat(exception.getErrorCode())
                    .isEqualTo(BusinessErrorCode.{ERROR_CODE});
        }
    }
}
```

- repository 를 **mock 하지 않는다** — `DatabaseCleaner` 로 격리된 실제 H2 사용.
- `BaseServiceTest` 는 `protected DebateRepository debateRepository` 를 노출(필요 시 사전 데이터 준비용).

## Persistence(DomainRepository 어댑터) 테스트 패턴

`infrastructure.persistence.*`. 도메인 repository 인터페이스 구현체(어댑터)를 검증. `BaseDomainRepositoryTest` 가 `protected DebateJpaRepository debateJpaRepository` 를 노출하므로 영속화 결과를 직접 조회해 검증할 수 있다.

```java
public class DebateDomainRepositoryTest extends BaseDomainRepositoryTest {

    @Autowired
    private DebateDomainRepository debateDomainRepository;

    @Nested
    class Create {

        @Test
        void 토론을_저장하고_식별자가_부여된_도메인을_반환한다() {
            Debate debate = new Debate(null, "토론 주제");

            Debate created = debateDomainRepository.create(debate);

            assertAll(
                    () -> assertThat(created.getId()).isNotNull(),
                    () -> assertThat(created.getTopic()).isEqualTo("토론 주제")
            );
        }

        @Test
        void 저장한_토론이_실제로_영속화된다() {
            Debate debate = new Debate(null, "토론 주제");

            Debate created = debateDomainRepository.create(debate);

            DebateEntity persisted = debateJpaRepository.findById(created.getId()).orElseThrow();
            assertThat(persisted.getTopic()).isEqualTo("토론 주제");
        }
    }
}
```

## Jpa Repository 테스트 패턴

`infrastructure.persistence.jpa.*`, `BaseJpaRepositoryTest`(`@DataJpaTest` slice). JPA 기본 제공 CRUD 는 테스트하지 않는다 — **커스텀 쿼리/메서드만**.

```java
class {Entity}JpaRepositoryTest extends BaseJpaRepositoryTest {

    @Autowired
    private {Entity}JpaRepository repository;

    @Nested
    class {QueryMethodName} {

        @Test
        void 조건에_맞는_결과를_조회한다() {
            // @DataJpaTest 는 기본 롤백 — 엔티티 직접 저장 후 조회 검증
        }
    }
}
```

## Domain 단위 테스트 패턴

```java
class {Entity}Test {

    private static final Speaker DEFAULT_SPEAKER = new Speaker("speakerA", Side.PRO);

    @Nested
    class Validate {

        @ParameterizedTest
        @NullAndEmptySource
        void 필드가_비어있으면_예외를_던진다(String value) {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> new {Entity}(/* args with invalid value */));

            assertThat(exception.getErrorCode())
                    .isEqualTo(BusinessErrorCode.{ERROR_CODE});
        }

        @Test
        void 정상적으로_생성된다() {
            assertThatCode(() -> new {Entity}(/* valid args */))
                    .doesNotThrowAnyException();
        }
    }
}
```

## infra-* client adapter 테스트

`SttClient` / `LlmClient` 구현체는 외부 호출 비용 때문에 별도 가이드:

- **단위 테스트**: 벤더 SDK 응답을 mock해서 어댑터 → 도메인 타입 변환만 검증. SDK 타입(`OpenAIResponse`, AWS SDK 타입 등)은 시그니처 노출 금지지만 테스트 내부에서 mock 객체로 사용 가능.
- **mock profile 통합 테스트**: `@ConditionalOnProperty(name="llm.mode", havingValue="mock")` 빈이 fixture 응답을 돌려주는지 검증. 실제 외부 호출은 하지 않음 (CI 비용/rate limit).
- **외부 client 실 호출 테스트는 작성 금지**. CI 비용/rate limit 때문.

## Assertion 컨벤션

- **AssertJ** (`assertThat`) 기본.
- 응답의 여러 필드를 검증할 때 **`assertAll()`** (JUnit 5).
- 예외 검증은 **`assertThrows()`** → `assertThat`으로 error code 검증.
- 예외 없음 검증은 **`assertThatCode(...).doesNotThrowAnyException()`**.
- null/empty 검증은 **`@ParameterizedTest` + `@NullAndEmptySource`**.
- Document 테스트는 상태코드 검증(`.then().statusCode(...)`)이 핵심 — 본문 값 단언은 Controller 테스트의 몫.

## Static import (테스트 코드에 한해 허용)

필요한 것만:

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static io.restassured.RestAssured.given;
// Document 테스트
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
```

## 핵심 규칙 요약

- **소스 클래스를 먼저 읽고** public 메서드의 모든 분기를 파악.
- **REST API(Controller) 구현 시 Controller 테스트 + Document 테스트를 한 쌍으로 작성** — 둘 다 통과 + 스니펫 생성 확인.
- **public 메서드 / endpoint 당 @Nested 1개**.
- **각 레이어는 자기 로직만 테스트** — 호출하는 하위 메서드의 happy case 는 통과한다고 가정.
- **Controller/Service/Persistence 통합 테스트에서 협력자 mock 금지** — 실제 빈 + `DatabaseCleaner` 격리. 현재는 데이터도 `new` 로 직접 생성(generator 미도입).
- **Document 테스트에서만 service 를 `@MockitoBean` 으로 mock** — DB 미접근, 문서화에 집중. stub 인자는 `any(...)`.
- **Document 테스트의 실패 케이스는 코드 1개여도 항상 `@ParameterizedTest` + `@EnumSource(ErrorCode.class)`.** 요청 본문 검증 에러는 본문 필드 없는 `errorRequestDocument` 로 문서화(타입 불일치 회피).
- **테스트에 `@Transactional` 금지** — `DatabaseCleaner` 가 격리.
- **`final` 키워드 금지** (메서드 파라미터/로컬/클래스). 인스턴스 필드/`static final` 상수는 허용.
- **Base 클래스는 `public`** 으로 — 하위 패키지 상속 보장.
- 작성 후 **실행하여 통과 확인**:
  ```
  ./gradlew :{module}:test --tests "com.debatetracker.{...}.{TestClass}"
  ```

## 사용자에게 다시 물어봐야 하는 상황

다음 경우엔 자동 생성을 멈추고 사용자에게 확인한다:

- target의 모듈/레이어가 위 표에 매칭되지 않을 때
- Base 클래스가 프로젝트에 아직 없을 때 (임의 생성 금지)
- Generator 가 필요해 보일 때 (현재 미도입 — 직접 `new` 로 갈지, 신규 generator 를 같이 만들지)
- Document 테스트에서 mock 할 service 가 `BaseDocumentTest` 에 없고, Base 에 추가할지 해당 테스트에 둘지 애매할 때
- 인증/인가가 필요한 endpoint 인데 아직 인증 인프라가 없을 때
- target 이 외부 API/Client 호출을 포함해 mock 전략이 불명확할 때
- public 메서드가 너무 많거나 책임이 섞여 있어 Nested 구조가 어색할 때 (리팩터링 제안 의도)
