# app-report — CLAUDE.md

> 루트 [CLAUDE.md](../CLAUDE.md) 와 [docs/multi-module-strategy.md](../docs/multi-module-strategy.md) 를 먼저 읽고 와야 한다.

## 현재 상태: 스켈레톤만 유지

**Sprint 3-4 (F2 사후 분석) 진입 시점에 본격 개발 시작.** 그 전까지 이 모듈은:

- `ReportApplication` (빈 `@SpringBootApplication`) + `application.yaml` (`spring.application.name: app-report`) 만 존재.
- Sprint 1-2 동안 **이 모듈에 새 기능을 추가하지 않는다** (§1).
- 빌드는 깨지지 않게 유지. CI 에서 `:app-report:compileJava` 는 계속 통과해야 한다.

## 이 모듈이 책임질 것 (Sprint 3+ 진입 시)

[docs/plan.md](../docs/plan.md) F2/F3 절 참조.

- **F2 사후 분석** — 팀별 분석 (설득력 점수, 토론 피드백, 쟁점별 우세 판정), 개인별 분석 (발언 분석, 최고/취약 발언). 토론 종료 직후 자동 트리거.
- **F3-1 카드뉴스** — 동아리 활동 내역 정리용. 템플릿 커스터마이즈. on-demand.
- **F3-2 세특 초안** — 학생별 개인 퍼포먼스 기반 세특 문장 자동 추출. on-demand.

## 의존성

`app-report/build.gradle`:

```gradle
implementation project(':infra-llm')
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
runtimeOnly 'com.h2database:h2'
```

- **`infra-stt` 의존 없음** — 사후 분석은 정제된 발화 텍스트에서 시작, 음성 처리는 안 함.
- **`app-debate` 의존 금지** (§2.2). DB 만 공유.

## DB 접근 — Read-Only (§2.5)

`app-debate` 가 발화·정제본·쟁점 트리의 **소유자**다. 이 모듈은:

- **Flyway 마이그레이션 실행 금지.** 스키마 변경 권한 없음.
- MySQL 계정 분리 + 코드 repository 분리 양쪽으로 read-only 강제 (구체 방식은 §6.2 미정 — Sprint 3 진입 시 결정).
- 새 데이터를 만들고 싶다면 자체 테이블을 추가하되, `app-debate` 가 소유한 테이블은 read 만.
- `app-debate` 의 스키마 변경은 이 모듈의 read DTO/매핑에 즉시 영향 → 마이그레이션 PR 에 양쪽 영향 확인 필수.

## use case wrapper 패턴 (Sprint 3+ 진입 시 §2.4)

`infra-llm` 의 `LlmClient` 를 직접 컨트롤러/서비스에서 호출하지 말고 도메인 wrapper 를 둔다:

- `LectureRecordDraftGenerator` (F3-2 세특 초안)
- `CardNewsComposer` (F3-1 카드뉴스)
- `TeamAnalysisGenerator` / `IndividualAnalysisGenerator` (F2)

`app-debate` 의 `UtteranceCorrector` / `IssueTreeExtractor` 와 코드를 공유하지 않는다 — 중복 적극 수용 (§1).

## Sprint 3 진입 시 결정해야 할 것 (§6.2 Open Questions)

- read-only 강제 방식 — MySQL 계정 분리 + 코드 repository 분리, 어느 단계부터 둘 다 적용할지.
- DB 분리 시점·전략 — `app-report` 가 자체 DB 로 가질 데이터의 ETL/CDC/이벤트 경로.
- 로컬 dev compose — `app-report` 개발 본격 진입 직전.
- F2/F3 가 `infra-llm` 을 어떻게 소비할지 — `LlmClient` 인터페이스가 F1 만으로 결정되었을 가능성 → 추가 시그니처 필요 여부 확인.

## 부트 실행

```powershell
.\gradlew :app-report:bootRun     # Sprint 3+ 에서나 의미 있음
.\gradlew :app-report:bootJar
```
