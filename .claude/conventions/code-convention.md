# Java 코드 스타일 컨벤션 (팀 공통)

> debate-tracker-backend 팀 Java/Spring 코드 스타일. `api-scaffold`, `test-scaffold` 스킬이 이 문서를 단일 출처로 삼는다. 기본적으로 [Woowacourse Style Guide](https://github.com/woowacourse/woowacourse-docs/tree/main/styleguide/java) 를 따른다.

## 1. 코드 포맷팅

### 1.1 개행 규칙

- **클래스 선언 직후 1줄 개행**.
- **return 문 전에는 개행하지 않는다.**
- **상수와 필드 사이에는 개행**한다.

### 1.2 어노테이션 정렬

- 옵션을 제외한 **어노테이션 길이 순**으로 선언한다. 예: `@Entity`(6) → `@NoArgsConstructor`(13).

### 1.3 메서드 체이닝

- 한 줄에 하나의 점(`.`)으로 체이닝한다 (stream, builder 포함).
- 예외: 필드 접근 또는 디미터 법칙 준수 시 한 줄에 여러 점 허용.

## 2. 메서드 & 필드 설계

### 2.1 Final 키워드

- 메서드 파라미터, 로컬 변수, 클래스에 `final` 키워드 **사용 금지** (인스턴스 필드 `final`, 상수 `static final` 은 허용).

### 2.2 메서드 순서

1. Public 메서드 먼저 선언.
2. Public 과 관련된 Private 메서드는 바로 아래에 위치.
3. 여러 Public 에서 쓰는 공통 Private 메서드는 맨 아래.
4. CRUD 순서로 정렬 (Create → Read → Update → Delete).

### 2.3 생성자 설계

- 매개변수가 한 줄을 넘으면 각 매개변수에 개행.
- 생성자가 여러 개면 생성자 체이닝 사용. 초기화 이전 작업(변환/검증)은 기본 생성자에서 수행. 부득이하면 의도 코멘트 작성.

## 3. 타입별 컨벤션

### 3.1 Entity

- `@Getter` 사용.
- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- `equals` 재정의 지양. 재정의하더라도 id 값 비교로만 제한.
- Embedded 필드명과 클래스명을 동일하게 하지 않는다 (예: `Nickname` 클래스 → `memberNickname` 필드).

### 3.2 DTO

- DTO 는 `record` 클래스 사용.
- 요청 DTO: `xxxRequest` (구체적 메서드명 포함, 저장은 `xxxSaveRequest`). 응답 DTO: `xxxResponse`.
- `List<>` 응답은 객체로 한 번 더 감싸 반환 (`MembersResponse(List<MemberResponse> members)`).
- 검증 분리: **입력 검증(형식·null)은 DTO**, **비즈니스 정책 검증은 도메인(Entity/Service)**.

### 3.3 Record 정적 팩토리 메서드

- Record 형태의 DTO 에서만 정적 팩토리 메서드를 **허용**한다.
- 일반 Entity/Domain 클래스에서는 **금지** (§6.1 참조).

## 4. 프레임워크 & 라이브러리

### 4.1 Lombok

- **허용**: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`(Entity 만), `@RequiredArgsConstructor`(Entity 아닌 객체), `@Builder`, `@Slf4j`.
- **금지**: `@AllArgsConstructor`, `@RequiredArgsConstructor`(Entity 에 한정 금지), `@EqualsAndHashCode`, `@Data`, `@Value`, `@Log`.

### 4.2 Static Import

- 비즈니스 코드: **금지**.
- 테스트 코드: **허용** (AssertJ, JUnit, Mockito, RestDocs 등).

## 5. 네이밍 컨벤션

### 5.1 클래스명

| 타입 | 규칙 | 예시 |
|---|---|---|
| Entity | PascalCase | `Member`, `Station` |
| DTO (Request) | `xxxRequest` | `MemberSaveRequest` |
| DTO (Response) | `xxxResponse` | `MembersResponse` |
| Service | `xxxService` | `MemberService` |
| Repository | `xxxRepository` | `MemberRepository` |
| Exception | `xxxException` | |

### 5.2 메서드명 (CRUD 기준)

- Create: `save`, `create`
- Read: `findById`, `findAll`, `getById`
- Update: `update`
- Delete: `delete`, `deleteById`

조회는 `Optional<{Domain}>` 반환을 기본으로 한다.

### 5.3 변수명

- 상수: `UPPER_SNAKE_CASE`, 필드/변수: `camelCase`. 한글 약자 금지.

## 6. 아키텍처

### 6.1 정적 팩토리 메서드 규칙

- 일반 Entity/Domain 클래스에서는 정적 팩토리 메서드 **금지**. Record DTO 에서만 허용.

### 6.2 트랜잭션 어노테이션

- `@Transactional(readOnly = true)` 또는 `@Transactional` 은 **메서드 위에** 붙인다. **클래스 위 금지.**

## 7. 테스트 코드 컨벤션

- `UI 를 제외한 모든 코드는 테스트한다` 기준. 단, JPA 기본 제공 CRUD, 단순 전달 메서드는 제외.
- 테스트 자원은 Docker 로 띄운다 (CI: Testcontainers, Local: Docker 사전 기동).
- 메서드 네이밍: 상위 `@Nested` 클래스명은 메서드명, 하위 테스트명은 **한글**.
  - 성공: `{기능}을_할_수_있다`, `{결과}를_반환한다`
  - 예외: `{조건}이면_예외가_발생한다`, `존재하지_않는_{객체}이면_예외를_던진다`
  - 필터링: `{조건}으로_필터링하여_조회할_수_있다`
- `given-when-then` 주석은 달지 않고, 빈 줄로 단계를 구분한다.
- 공통 더미 데이터 없이 각 테스트 케이스마다 더미 데이터를 추가한다.
- 각 layer 의 테스트 환경은 `BaseXXXTest` 로 설정하고 상속받아 작성한다.
  - Domain: 단위 테스트
  - Repository: `@DataJpaTest`
  - Service: `@SpringBootTest(webEnvironment = NONE)` (h2)
  - Controller: `@SpringBootTest(webEnvironment = RANDOM_PORT)` 통합 테스트
- DB 격리는 `@Transactional` 이 아닌 `DatabaseCleaner`(`BeforeEachCallback` 구현체)로 매 테스트 전 truncate 한다.
