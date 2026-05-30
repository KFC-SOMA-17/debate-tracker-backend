# Git 컨벤션 (팀 공통)

> debate-tracker-backend 팀 git/커밋/PR 규약. `/commit`, `/pr` 명령어가 이 문서를 단일 출처로 삼는다.

## 커밋 메시지

형식:

```
<prefix>: <한국어 설명>

<선택: body>
```

- 예시: `config: 백엔드 프로젝트 환경 설정`
- 제목은 한국어, 명사형 종결, 마침표 없음. 전체 히스토리가 한국어이므로 영어 메시지 금지.

### prefix

| prefix | 의미 |
|---|---|
| `feat` | 기능 개발 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없이 코드 구조 변경 (예: 필드 이름 변경) |
| `test` | 테스트 코드 관련 작업 |
| `docs` | 문서 관련 작업 |
| `chore` | 파일 내부가 아닌 파일 자체에 대한 변경 (폴더 이동/변경, 파일 이름 변경/삭제) |
| `style` | 코드 컨벤션 관련 작업 (포맷팅, 어노테이션 순서 등) |
| `config` | 외부 라이브러리 추가 및 설정 (`build.gradle` 등) |
| `design` | UI 관련 개발 및 수정 |

### body

커밋 단위가 너무 커지면 body 를 작성한다.

```
config: 백엔드 프로젝트 환경 설정

- 커밋에 대한 구체적인 설명1
- 커밋에 대한 구체적인 설명2
```

## 브랜치

| 브랜치명 | 설명 | 예시 | 규칙 |
|---|---|---|---|
| `main` | 배포를 위한 브랜치 | | 삭제 X |
| `develop` | 기능 개발을 위한 브랜치 (default) | | 삭제 X |
| `feat/#이슈번호` | develop 에서 분기, 이슈 단위 개발 (`feat/`, `refactor/`, `test/` 등) | `feat/#10` | merge 후 삭제 |
| `hotfix/#이슈번호` | 급한 이슈 발생 시 main 에서 분기 | `hotfix/#4` | merge 후 삭제 |

## PR / 코드 리뷰

### Merge 규칙

- 각 파트 전원의 Approve 를 받아야 한다.
- `feat → develop` : **Squash and merge**
- `develop ↔ main` : **merge with commit**
- 그 외 : 일반 merge

### 코드 리뷰

- 코멘트 확인 시 이모지를 달아둔다.
- 오프라인 대화의 결론은 코멘트로 남긴다.

### PR 제목

- 형식: `<prefix>: <한국어 설명>` (커밋과 동일), 마침표 없음, 명사형 종결.

### PR 옵션 설정

- Reviewers 를 할당한다.
- Assignee 를 할당한다.
- Label 을 할당한다. (라벨 순서: **파트 - 워크플로우** — feature, refactor 등)
- Project 를 할당한다.
- Milestone 은 이슈에서 상속될 수 있다.
- `/noti` 로 웹 훅(디스코드) 공지 가능.

### PR 본문 템플릿

`.github/pull_request_template.md` 구조를 유지한다. 기본 형태:

```markdown
# 🚩 연관 이슈
closed #

# 🗣️ 리뷰 요구사항 (선택)
```

## 이슈

- 이슈 제목은 커밋과 구분되게 `[#헤더]` 를 붙인다. 예: `[FEAT] 이슈내용`.
- 마일스톤과 Project 를 할당한다.

## Tag (백엔드 버전)

`v{major 기능 추가}.{minor 기능 추가}.{hotfix}`

- 예: `v1.1.0`
