---
name: test-scaffold
description: debate-tracker-backend 팀 컨벤션에 맞춰 Java/Spring 테스트 코드를 작성한다. Nested 클래스 + 한글 메서드명 + Base 클래스 상속 + Generator 기반 fixture 패턴.
---

# test-scaffold

지정한 소스 클래스/메서드에 대해 팀 테스트 컨벤션(`docs/convention.md`, `docs/test-skill-example.md`)에 맞는 테스트를 생성한다.

## 입력

- **target**: 테스트할 소스 클래스 또는 메서드 (예: `SessionService`, `SessionController#createSession`).
- **type** (선택): `controller` | `service` | `persistence` | `repository` | `domain` | `document`. 생략 시 target 클래스의 패키지로 자동 판별.

## 사전 작업

### 1. 모듈 식별

target 클래스의 경로로 모듈을 식별:

| 경로 prefix | 모듈 |
|---|---|
| `app-debate/src/main/java/com/debatetracker/debate/**` | `app-debate` |
| `app-report/src/main/java/com/debatetracker/report/**` | `app-report` |
| `infra-stt/src/main/java/com/debatetracker/infra/stt/**` | `infra-stt` |
| `infra-llm/src/main/java/com/debatetracker/infra/llm/**` | `infra-llm` |

`docs/module-convention.md` 기준. 패키지 베이스는 `com.debatetracker.{module}`.

### 2. 모듈별 가능한 레이어

| 모듈 | 가능한 레이어 |
|---|---|
| `app-debate` | controller, service, persistence, repository, domain, document |
| `app-report` | controller, service, persistence, repository, domain, document |
| `infra-stt` | (client adapter) domain 단위 테스트, mock profile 통합 테스트 |
| `infra-llm` | (client adapter) domain 단위 테스트, mock profile 통합 테스트 |

`infra-*`는 controller/repository가 없음. WebSocket/REST는 `app-*`에만 존재.

### 3. 테스트 파일 위치

소스 패키지 구조를 미러링:

```
app-debate/src/main/java/com/debatetracker/debate/service/SessionService.java
  → app-debate/src/test/java/com/debatetracker/debate/service/SessionServiceTest.java
```

### 4. 소스 우선 읽기

테스트 작성 전 **반드시 target 클래스의 모든 public 메서드, 파라미터, 반환 타입, 던질 수 있는 예외를 파악**한다. private helper에 분기가 있다면 그 분기도 호출 메서드의 edge case로 묶어 테스트 대상.

## 레이어별 Base 클래스 & 어노테이션

| 레이어 | Base 클래스 | WebEnvironment | 핵심 어노테이션 |
|---|---|---|---|
| Controller (`{module}.controller.*`) | `BaseControllerTest` | `RANDOM_PORT` | `@ExtendWith(DatabaseCleanerExtension.class)` |
| Service (`{module}.service.*`) | `BaseServiceTest` | `NONE` | `@ExtendWith(DatabaseCleanerExtension.class)` |
| Persistence (`{module}.persistence.*`) | `BasePersistenceTest` | `NONE` | `@ExtendWith(DatabaseCleanerExtension.class)` |
| Repository (`{module}.repository.*`) | `BaseRepositoryTest` | N/A | `@DataJpaTest` |
| Domain (`{module}.domain.*`) | 없음 (plain JUnit) | N/A | N/A |
| Document (`{module}.document.*`) | `BaseDocumentTest` | `RANDOM_PORT` | `@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})` |

**Base 클래스가 아직 프로젝트에 없으면**:
- target 파일을 작성하되 상단 주석으로 `// TODO: BaseServiceTest 도입 후 상속으로 전환` 같이 남기지 말고, **사용자에게 알리고 진행 여부를 묻는다**. 베이스 클래스 도입은 인프라 결정이므로 임의 생성 금지.

## 테스트 클래스 구조

```java
class {ClassName}Test extends {BaseClass} {

    @Autowired
    private {TargetClass} {targetField};  // Service/Persistence/Repository/Controller

    @Nested
    class {MethodName} {     // public 메서드당 @Nested 1개

        @Test
        void 한글로_테스트_의도를_설명한다() {
            // Arrange - Generator로 데이터 생성

            // Act - 테스트 대상 메서드 실행

            // Assert - AssertJ/JUnit으로 검증
        }
    }
}
```

**중요:**
- `given-when-then` 주석 **금지**, 대신 빈 줄로 단계를 구분 (팀 컨벤션 `docs/convention.md`).
- `final` 키워드는 메서드 파라미터/로컬 변수/클래스에 사용 **금지**.
- 테스트 클래스/메서드에 `@Transactional` 붙이지 않음 (DatabaseCleaner가 격리 담당).
- `final` 변수 선언 금지(상수 `static final` 제외).

## 메서드 네이밍

한글 + 언더스코어. 패턴:

- 성공: `{기능}을_할_수_있다`, `{결과}를_반환한다`
- 예외: `{조건}이면_예외가_발생한다`, `존재하지_않는_{객체}이면_예외를_던진다`
- 필터링: `{조건}으로_필터링하여_조회할_수_있다`

예시 (debate-tracker 도메인):
- `세션이_준비_상태일_때_시작할_수_있다`
- `존재하지_않는_세션이면_예외를_던진다`
- `같은_화자의_발화는_연속으로_묶인다`

## Fixture / Generator

도메인 fixture는 `{module}.fixture` 패키지의 `*Generator` 클래스로 생성한다. eatda 예시의 도메인 generator는 **debate-tracker 도메인 용어로 교체**한다.

예상되는 generator (실제 코드에 존재할 때만 사용):

| Generator | 용도 |
|---|---|
| `sessionGenerator.generate(...)` | 토론 세션 |
| `utteranceGenerator.generate(...)` | 발화 |
| `speakerGenerator.generate(...)` | 화자 |
| `issueGenerator.generate(...)` | 쟁점 |
| `issueTreeGenerator.generate(...)` | 쟁점 트리 |

**사용 규칙:**
- 모든 generator는 base test class에서 `protected` 필드로 노출된다고 가정.
- 통합 테스트(Service/Persistence/Repository/Controller)에서는 `new`로 엔티티 생성 **금지** — 항상 generator 사용.
- domain 단위 테스트에서는 `new` 사용 가능.
- **generator가 프로젝트에 아직 없으면** 작성하지 말고, 테스트 코드에 TODO 주석 대신 사용자에게 알린다. fixture 도입은 모듈/도메인 합의 사항.

## Controller 테스트 패턴 (RestAssured)

```java
class {Controller}Test extends BaseControllerTest {

    @Nested
    class {EndpointMethod} {

        @Test
        void 성공_케이스를_검증한다() {
            Speaker speaker = speakerGenerator.generate("speakerA");
            Session session = sessionGenerator.generate(speaker);

            {ResponseType} response = given()
                    .header(HttpHeaders.AUTHORIZATION, accessToken(speaker))
                    .contentType(ContentType.JSON)
                    .body(request)
                    .pathParam("id", session.getId())
                    .when()
                    .get("/api/sessions/{id}")
                    .then()
                    .statusCode(200)
                    .extract().as({ResponseType}.class);

            assertAll(
                    () -> assertThat(response.field1()).isEqualTo(expected1),
                    () -> assertThat(response.field2()).isEqualTo(expected2)
            );
        }

        @Test
        void 인증되지_않은_요청은_401을_반환한다() {
            given()
                    .when()
                    .get("/api/sessions/1")
                    .then()
                    .statusCode(401);
        }
    }
}
```

## Service 테스트 패턴

```java
class {Service}Test extends BaseServiceTest {

    @Autowired
    private {ServiceClass} {serviceField};

    @Nested
    class {MethodName} {

        @Test
        void 정상_동작한다() {
            Session session = sessionGenerator.generate(...);

            {ReturnType} result = {serviceField}.{method}(args);

            assertAll(
                    () -> assertThat(result.field()).isEqualTo(expected)
            );
        }

        @Test
        void 존재하지_않으면_예외를_던진다() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> {serviceField}.{method}(invalidArgs));

            assertThat(exception.getErrorCode())
                    .isEqualTo(BusinessErrorCode.{ERROR_CODE});
        }
    }
}
```

## Persistence 테스트 패턴

```java
class {Persistence}Test extends BasePersistenceTest {

    @Autowired
    private {PersistenceClass} {persistenceField};

    @Nested
    class {MethodName} {

        @Test
        void 정상_동작한다() {
            Session session = sessionGenerator.generate(...);

            {ReturnType} result = {persistenceField}.{method}(args);

            assertAll(
                    () -> assertThat(result.field()).isEqualTo(expected)
            );
        }

        @Test
        void 조건에_해당하면_예외를_던진다() {
            assertThatThrownBy(() -> {persistenceField}.{method}(args))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(BusinessErrorCode.{ERROR_CODE}.getMessage());
        }
    }
}
```

## Repository 테스트 패턴

JPA 기본 제공 CRUD는 테스트하지 않는다 (`docs/convention.md` 테스트 컨벤션). 커스텀 쿼리/메서드만.

```java
class {Repository}Test extends BaseRepositoryTest {

    @Autowired
    private {RepositoryClass} {repositoryField};

    @Nested
    class {QueryMethodName} {

        @Test
        void 조건에_맞는_결과를_조회한다() {
            Session session = sessionGenerator.generate(...);

            List<Session> actual = {repositoryField}.{method}(args);

            assertThat(actual).map(Session::getId)
                    .containsExactlyInAnyOrder(session.getId());
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
- **mock profile 통합 테스트**: `@ConditionalOnProperty(name="llm.mode", havingValue="mock")` 빈이 fixture 응답을 돌려주는지 검증. 실제 외부 호출은 하지 않음 (`docs/module-convention.md` §5.2 (5)).
- **외부 client 실 호출 테스트는 작성 금지**. CI 비용/rate limit 때문. 별도 staging 검증 채널이 합의되면 그때 도입.

## Assertion 컨벤션

- **AssertJ** (`assertThat`) 기본.
- 응답의 여러 필드를 검증할 때 **`assertAll()`** (JUnit 5).
- 예외 검증은 **`assertThrows()`** → `assertThat`으로 error code 검증.
- 예외 없음 검증은 **`assertThatCode(...).doesNotThrowAnyException()`**.
- null/empty 검증은 **`@ParameterizedTest` + `@NullAndEmptySource`**.

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
import static io.restassured.RestAssured.given;
```

## 핵심 규칙 요약

- **소스 클래스를 먼저 읽고** public 메서드의 모든 분기를 파악.
- **public 메서드당 @Nested 1개**.
- **각 레이어는 자기 로직만 테스트** — 호출하는 하위 메서드의 happy case는 통과한다고 가정, edge case는 해당 메서드 본인 테스트에서. 단, private helper 분기는 호출 메서드의 edge case로 포함.
- **통합 테스트에서 repository/service mock 금지** — generator로 실제 데이터.
- **외부 client(SttClient, LlmClient)는 base 클래스에서 `@MockitoBean`으로 이미 mock** (도입 가정).
- **테스트에 `@Transactional` 금지** — DatabaseCleaner가 격리.
- **`final` 키워드 금지** (메서드 파라미터/로컬/클래스).
- 작성 후 **실행하여 통과 확인**:
  ```
  ./gradlew :{module}:test --tests "com.debatetracker.{...}.{TestClass}"
  ```

## 사용자에게 다시 물어봐야 하는 상황

다음 경우엔 자동 생성을 멈추고 사용자에게 확인한다:

- target의 모듈/레이어가 위 표에 매칭되지 않을 때
- Base 클래스가 프로젝트에 아직 없을 때
- Generator가 도메인에 없을 때 (직접 `new`로 작성할지, 신규 generator를 같이 만들지)
- target이 외부 API/Client 호출을 포함해 mock 전략이 불명확할 때
- public 메서드가 너무 많거나 책임이 섞여 있어 Nested 구조가 어색할 때 (리팩터링 제안 의도)
