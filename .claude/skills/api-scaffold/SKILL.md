---
name: api-scaffold
description: debate-tracker-backend 팀 컨벤션에 맞춰 REST API 한 개를 Controller → Service → Domain(port) → Infra(adapter) → JPA 5계층으로 스캐폴딩한다. 도메인별 수직 슬라이스 + DTO/Entity/Domain 변환 책임 분리 + 안쪽(도메인) 단방향 의존. app-* 모듈에 적용한다. 코드 생성 후 test-scaffold 로 테스트를 잇는다.
---

# api-scaffold

`app-*` 모듈에 REST API endpoint 하나를 팀 컨벤션(`.claude/conventions/code-convention.md`, 루트 `CLAUDE.md`)에 맞는 계층 구조로 생성한다. 이 스킬은 **프로덕션 코드**만 만든다. 테스트는 [test-scaffold](../test-scaffold/SKILL.md) 가 책임진다 — endpoint 를 만들었으면 반드시 test-scaffold 로 Controller 테스트 + Document 테스트를 잇는다.

## 입력

- **domain**: 도메인 이름 (예: `Debate`, `Session`, `Issue`). 이 이름이 `{Domain}`(PascalCase) / `{domain}`(소문자) 로 모든 파일·패키지에 파라미터화된다.
- **action**: endpoint 동작 (예: `Create`, `Get`, `Update`, `Delete`, `List`). DTO 이름(`{Domain}{Action}Request/Response`)과 HTTP 매핑을 결정한다.
- **module** (선택): 생략 시 `app-debate`. 현재 본격 개발 모듈.

## 적용 대상 모듈

| 모듈 | base 패키지 | 비고 |
|---|---|---|
| `app-debate` | `com.debatetracker.debate` | Sprint 1-2 본격 개발. **DB 스키마 owner (Flyway)** |
| `app-report` | `com.debatetracker.report` | Sprint 3+ 스켈레톤. **DB read-only** — write API 스캐폴딩 금지(§아래) |

- `infra-*` 는 REST/Controller 가 없다. 이 스킬 대상 아님 (벤더 중립 client adapter — 루트 `CLAUDE.md` §2.4).
- **Sprint 1-2 동안 `app-report` 에 write endpoint 를 만들지 않는다.** read-only 조회 API 만 허용되며, 그조차 `app-debate` 가 owner 인 스키마를 read 하는 형태여야 한다. write 가 필요하면 사용자에게 확인.

## 계층 구조 — 의존성은 항상 안쪽(도메인)으로

```
[Controller]   controller/{domain}/          DTO in/out
   │
   ▼
[Service]      service/{domain}/             Domain in/out
   │
   ▼
[Domain Port]  domain/{domain}/   ◄── {Domain}Repository (interface) + {Domain} (model)
   ▲
   │ implements
[Infra Adapter] infrastructure/persistence/  {Domain}DomainRepository  (Entity ↔ Domain 변환)
   │
   ▼
[JPA]          infrastructure/persistence/jpa/{domain}/
                  {Domain}JpaRepository (Spring Data) + {Domain}Entity (@Entity)
```

**절대 규칙:**

1. **Domain 은 아무것도 의존하지 않는다.** `{Domain}`, `{Domain}Repository`(interface) 는 JPA·Spring·Controller·DTO 를 import 하지 않는다.
2. **Service 는 도메인 port(인터페이스)에만 의존**한다. `{Domain}DomainRepository`(구현체)나 `{Domain}JpaRepository`(JPA)를 직접 모른다. 생성자에 `{Domain}Repository` 인터페이스를 주입받는다.
3. **Controller ↔ Service 는 도메인 객체로**, **Controller ↔ 외부는 DTO 로** 통신. DTO 가 Service 안쪽으로 새지 않는다.
4. **Entity 는 인프라 안에만 갇힌다.** `{Domain}Entity` 가 Service/Controller 시그니처에 노출되지 않는다.
5. (멀티모듈) **벤더 SDK 타입을 도메인이 직접 보지 않는다** — 필요하면 `infra-*` client wrapper 를 끼운다 (루트 `CLAUDE.md` §2.4).

## 패키지 / 파일 레이아웃

`{module}/src/main/java/com/debatetracker/{module-base}/` 하위:

```
controller/{domain}/{Domain}RestController.java
controller/{domain}/{Domain}{Action}Request.java          # 요청 DTO (record)
controller/{domain}/{Domain}{Action}Response.java         # 응답 DTO (record)
service/{domain}/{Domain}Service.java
domain/{domain}/{Domain}.java                             # 도메인 모델 (POJO)
domain/{domain}/{Domain}Repository.java                   # port (interface)
infrastructure/persistence/{Domain}DomainRepository.java  # adapter (port 구현체)
infrastructure/persistence/jpa/{domain}/{Domain}JpaRepository.java
infrastructure/persistence/jpa/{domain}/{Domain}Entity.java
```

**수직 슬라이스 응집 규칙:** 한 도메인의 `controller/{domain}` · `service/{domain}` · `domain/{domain}` · `jpa/{domain}` 은 **모두 같은 소문자 `{domain}` 패키지명**을 쓴다. Entity 와 JpaRepository 는 반드시 같은 `jpa/{domain}/` 에 둔다.

> ⚠️ **현재 코드 주의:** `DebateEntity` 는 옛 패키지 `jpa/transcript/` 에, `DebateJpaRepository` 는 `jpa/debate/` 에 흩어져 있다(리네임 잔재). 새 도메인은 이 실수를 반복하지 말고 `jpa/{domain}/` 한 곳에 모은다. Debate 를 손보게 되면 `DebateEntity` 도 `jpa/debate/` 로 이동 권장.

## 생성 순서 — 안쪽(도메인)부터 바깥으로

의존성 컴파일이 끊기지 않도록 **안쪽부터** 만든다:

1. `domain/{domain}/{Domain}.java` — 도메인 모델
2. `domain/{domain}/{Domain}Repository.java` — port
3. `infrastructure/persistence/jpa/{domain}/{Domain}Entity.java` — Entity
4. `infrastructure/persistence/jpa/{domain}/{Domain}JpaRepository.java` — Spring Data
5. `infrastructure/persistence/{Domain}DomainRepository.java` — adapter
6. `service/{domain}/{Domain}Service.java` — 비즈니스 로직
7. `controller/{domain}/{Domain}{Action}Request.java` / `{Domain}{Action}Response.java` — DTO
8. `controller/{domain}/{Domain}RestController.java` — endpoint

## 변환(매핑) 책임 — "각 경계 변환은 안쪽을 아는 바깥쪽이 책임진다"

| 변환 | 위치 | 형태 |
|---|---|---|
| Request → Domain | `{Domain}{Action}Request.toDomain()` | record 인스턴스 메서드 |
| Domain → Response | `new {Domain}{Action}Response(domain)` | record 변환 생성자 |
| Domain → Entity | `new {Domain}Entity(domain)` | Entity 변환 생성자 |
| Entity → Domain | `{Domain}Entity.toDomain()` | Entity 인스턴스 메서드 |

> 일관 원칙: **DTO/Entity 변환은 "변환 생성자 + `toDomain()` 인스턴스 메서드"** 패턴으로 고정한다. (record DTO 는 정적 팩토리도 허용되지만 — `.claude/conventions/code-convention.md` §3.3 — 이 프로젝트는 변환 생성자로 통일되어 있어 호출부가 깔끔하다.) 일반 Entity/Domain 클래스에는 **정적 팩토리 사용 금지** (`.claude/conventions/code-convention.md` §6.1).

## 계층별 템플릿

아래는 `Debate` + `Create` 실제 코드 기준. `{Domain}`/`{domain}`/`{Action}`/`{field}` 를 치환한다.

### 1. Domain 모델 — `domain/{domain}/{Domain}.java`

순수 POJO. `@Getter` + `@RequiredArgsConstructor` (Entity 가 **아니므로** `@RequiredArgsConstructor` 허용 — `.claude/conventions/code-convention.md` §4.1). JPA·Spring import 금지. 비즈니스 정책 검증은 여기.

```java
package com.debatetracker.{module-base}.domain.{domain};

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class {Domain} {

    private final Long id;
    private final String {field};
}
```

### 2. Repository port — `domain/{domain}/{Domain}Repository.java`

도메인 타입 in/out 인터페이스. 구현·JPA 를 모른다. 메서드명은 CRUD 컨벤션(`save`/`create`, `findById`/`findAll`, `update`, `deleteById` — `.claude/conventions/code-convention.md` §5.2). 조회는 `Optional<{Domain}>` 반환.

```java
package com.debatetracker.{module-base}.domain.{domain};

public interface {Domain}Repository {

    {Domain} create({Domain} {domain});
}
```

### 3. Entity — `infrastructure/persistence/jpa/{domain}/{Domain}Entity.java`

`@Entity` `@Getter` `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. **어노테이션은 길이 순 정렬** (`.claude/conventions/code-convention.md` §1.2). `@RequiredArgsConstructor` **금지**(Entity). domain→entity 변환 생성자 + entity→domain `toDomain()`. 입력 형식 제약은 `@NotBlank` 등.

```java
package com.debatetracker.{module-base}.infrastructure.persistence.jpa.{domain};

import com.debatetracker.{module-base}.domain.{domain}.{Domain};
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "{domain}")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class {Domain}Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String {field};

    public {Domain}Entity({Domain} domain) {
        this.id = domain.getId();
        this.{field} = domain.get{Field}();
    }

    public {Domain} toDomain() {
        return new {Domain}(id, {field});
    }
}
```

### 4. JpaRepository — `infrastructure/persistence/jpa/{domain}/{Domain}JpaRepository.java`

Spring Data. 기본 CRUD 외 커스텀 쿼리만 추가.

```java
package com.debatetracker.{module-base}.infrastructure.persistence.jpa.{domain};

import org.springframework.data.jpa.repository.JpaRepository;

public interface {Domain}JpaRepository extends JpaRepository<{Domain}Entity, Long> {
}
```

### 5. Adapter — `infrastructure/persistence/{Domain}DomainRepository.java`

`@Component` `@RequiredArgsConstructor`. port 구현체. JpaRepository 호출 + Entity ↔ Domain 변환만. 비즈니스 로직 금지.

```java
package com.debatetracker.{module-base}.infrastructure.persistence;

import com.debatetracker.{module-base}.domain.{domain}.{Domain};
import com.debatetracker.{module-base}.domain.{domain}.{Domain}Repository;
import com.debatetracker.{module-base}.infrastructure.persistence.jpa.{domain}.{Domain}Entity;
import com.debatetracker.{module-base}.infrastructure.persistence.jpa.{domain}.{Domain}JpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class {Domain}DomainRepository implements {Domain}Repository {

    private final {Domain}JpaRepository {domain}JpaRepository;

    @Override
    public {Domain} create({Domain} {domain}) {
        {Domain}Entity savedEntity = {domain}JpaRepository.save(new {Domain}Entity({domain}));
        return savedEntity.toDomain();
    }
}
```

### 6. Service — `service/{domain}/{Domain}Service.java`

`@Service` `@RequiredArgsConstructor`. **port 인터페이스에만** 의존. 비즈니스 정책 검증은 여기/도메인. 트랜잭션은 **메서드 위에** `@Transactional` / `@Transactional(readOnly = true)` (클래스 위 금지 — `.claude/conventions/code-convention.md` §6.2). 메서드 순서는 CRUD 순.

```java
package com.debatetracker.{module-base}.service.{domain};

import com.debatetracker.{module-base}.domain.{domain}.{Domain};
import com.debatetracker.{module-base}.domain.{domain}.{Domain}Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class {Domain}Service {

    private final {Domain}Repository {domain}Repository;

    public {Domain} create({Domain} {domain}) {
        return {domain}Repository.create({domain});
    }
}
```

### 7. DTO — `controller/{domain}/`

`record`. **입력 형식·null 검증은 DTO** (`@NotNull` 등), 비즈니스 정책 검증은 도메인 (`.claude/conventions/code-convention.md` §3.2). 요청은 `{Domain}{Action}Request`(저장은 관례상 `Create`), 응답은 `{Domain}{Action}Response`. **List 응답은 객체로 한 번 더 감싼다** (`{Domain}sResponse(List<{Domain}Response> {domain}s)`).

Request:

```java
package com.debatetracker.{module-base}.controller.{domain};

import com.debatetracker.{module-base}.domain.{domain}.{Domain};
import jakarta.validation.constraints.NotNull;

public record {Domain}{Action}Request(
        @NotNull String {field}
) {

    public {Domain} toDomain() {
        return new {Domain}(null, {field});
    }
}
```

Response:

```java
package com.debatetracker.{module-base}.controller.{domain};

import com.debatetracker.{module-base}.domain.{domain}.{Domain};

public record {Domain}{Action}Response(String {domain}Id, String {field}) {

    public {Domain}{Action}Response({Domain} {domain}) {
        this(Long.toString({domain}.getId()), {domain}.get{Field}());
    }
}
```

> 외부 노출 식별자는 현재 코드처럼 `String` 으로 변환(`Long.toString(...)`)한다. 내부 도메인 id 타입(Long)을 API 계약에 그대로 노출하지 않는 정책.

### 8. RestController — `controller/{domain}/{Domain}RestController.java`

`@RestController` `@RequiredArgsConstructor`. HTTP 매핑 + DTO↔도메인 변환 + Service 위임만. 로직 금지. 요청 본문 검증을 강제하려면 `@RequestBody` 에 `@Valid` 추가(검증 실패 시 `GlobalExceptionHandler` 가 `FIELD_ERROR` 400 으로 매핑).

```java
package com.debatetracker.{module-base}.controller.{domain};

import com.debatetracker.{module-base}.domain.{domain}.{Domain};
import com.debatetracker.{module-base}.service.{domain}.{Domain}Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class {Domain}RestController {

    private final {Domain}Service {domain}Service;

    @PostMapping("/api/{domain}s")
    public ResponseEntity<{Domain}{Action}Response> create{Domain}(@RequestBody {Domain}{Action}Request request) {
        {Domain} {domain} = {domain}Service.create(request.toDomain());
        return ResponseEntity.ok(new {Domain}{Action}Response({domain}));
    }
}
```

## HTTP 매핑 / action 별 가이드

| action | HTTP | 경로 | 요청 DTO | 응답 |
|---|---|---|---|---|
| Create | POST | `/api/{domain}s` | `{Domain}CreateRequest` | `{Domain}CreateResponse` |
| Get | GET | `/api/{domain}s/{id}` | `@PathVariable Long id` | `{Domain}Response` |
| List | GET | `/api/{domain}s` | `@RequestParam` 필터 | `{Domain}sResponse` (감싸기) |
| Update | PUT/PATCH | `/api/{domain}s/{id}` | `{Domain}UpdateRequest` | `{Domain}Response` |
| Delete | DELETE | `/api/{domain}s/{id}` | `@PathVariable Long id` | `ResponseEntity<Void>` |

- 조회 결과 없음은 Service/도메인에서 도메인 예외를 던지고 `GlobalExceptionHandler` 가 매핑한다 — Controller 에서 `Optional.get()` 호출 금지.

## 에러 / 검증 처리

- **입력 형식·null** → DTO 의 `@NotNull`/`@NotBlank` + Controller `@Valid`. 실패 시 `common` 모듈 `ErrorCode.FIELD_ERROR`(400).
- **비즈니스 정책** → 도메인/Service 에서 `DebateTrackerException(ErrorCode)` 던짐. 에러 코드는 `common` 의 `com.debatetracker.exception.ErrorCode` enum.
- **도메인 고유 에러 코드가 없으면** 임의로 만들지 말고 사용자에게 확인. 현재는 인프라 레벨 코드(`FIELD_ERROR` 400, `INTERNAL_SERVER_ERROR` 500 등)만 존재.
- 에러 응답 본문/상태 매핑은 전역 핸들러가 담당 — Controller 에서 try-catch 로 status 를 직접 만들지 않는다.

## 컨벤션 핵심 규칙 요약 (`.claude/conventions/code-convention.md`)

- **`final` 금지**: 메서드 파라미터/로컬 변수/클래스. (필드 `private final`, 상수 `static final` 은 허용.)
- **Lombok**: `@Getter`, `@NoArgsConstructor(PROTECTED)`(Entity만), `@RequiredArgsConstructor`(Entity 아닌 객체), `@Builder`, `@Slf4j` 만. `@Data`/`@Value`/`@AllArgsConstructor`/`@EqualsAndHashCode`/`@Log` **금지**. Entity 에 `@RequiredArgsConstructor` **금지**.
- **DTO 는 `record`**. 요청 `xxxRequest`(저장 `xxxSaveRequest`/`xxxCreateRequest`), 응답 `xxxResponse`. List 응답은 객체로 감싸기.
- **정적 팩토리**는 record DTO 에서만 허용. 일반 Entity/Domain 금지 → 변환 생성자 사용.
- **어노테이션 길이 순 정렬**(옵션 제외). 메서드 체이닝 한 줄에 한 점.
- **트랜잭션**은 메서드 위에만. 클래스 위 금지.
- **Static import 비즈니스 코드 금지** (테스트만 허용).
- **클래스 선언 직후 1줄 개행**, return 전 개행 금지, 상수/필드 사이 개행.
- **한글 약자 변수명 금지**, 상수 `UPPER_SNAKE_CASE`, 필드/변수 `camelCase`.

## DB 스키마 변경을 동반하면

- **`app-debate` 만 Flyway 실행** (스키마 owner). 새 Entity/컬럼은 마이그레이션 파일을 함께 만든다(1차 H2 → Sprint 진행 시 Postgres+Testcontainers 전환 예정).
- 스키마가 바뀌면 **PR 본문에 "양쪽 앱 영향 확인"** 체크 — `app-report` 의 read 엔티티/DTO 매핑도 같이 손봐야 할 수 있다(루트 `CLAUDE.md` §2.5).

## 코드 생성 후 — 반드시 테스트로 잇는다

endpoint 를 만들었으면 [test-scaffold](../test-scaffold/SKILL.md) 로 다음을 생성한다(생략 금지):

- **Controller 테스트** (`controller.{domain}.{Domain}RestControllerTest`) — 실제 빈 end-to-end.
- **Document 테스트** (`document.{domain}.{Domain}DocumentTest`) — RestDocs 스니펫.
- 새 layer 가 비자명 로직을 가지면 Service / Persistence / Domain 테스트도.

완료 정의(DoD): 두 테스트 통과 + `build/generated-snippets/{domain}/{action}/{status}/` 스니펫 생성 확인. 컴파일 확인:

```
.\gradlew :{module}:compileJava
.\gradlew :{module}:test --tests "com.debatetracker.{module-base}.controller.{domain}.{Domain}RestControllerTest"
```

## 사용자에게 다시 물어봐야 하는 상황

다음 경우엔 자동 생성을 멈추고 확인한다:

- `app-report` 에 **write endpoint** 를 만들어야 할 때 (Sprint 1-2 read-only 원칙 위반 — §적용 대상 모듈).
- **DB 스키마 변경**이 필요한데 Flyway 마이그레이션 정책/네이밍이 불명확할 때.
- **도메인 고유 에러 코드**가 필요한데 `common` 의 `ErrorCode` 에 없을 때 (임의 추가 금지 — common 모듈 변경은 양쪽 앱 영향).
- 도메인 모델에 **식별자·VO 중복**이 누적될 때 (식별자 3종 + VO 5종 이상 또는 동일 enum 2회 → `common-domain` 신설 논의 트리거, 루트 `CLAUDE.md`).
- endpoint 가 **외부 STT/LLM 호출**을 동반할 때 — Service 가 `SttClient`/`LlmClient` 를 직접 부르지 말고 use case wrapper(`UtteranceCorrector` 패턴)를 끼워야 하므로 설계 확인 필요.
- **인증/인가**가 필요한 endpoint 인데 인증 인프라가 아직 없을 때.
- Entity/JpaRepository 가 **기존 옛 패키지**(`jpa/transcript/` 등)에 흩어져 있어 새 도메인 배치와 충돌할 때 (이동 여부 확인).
