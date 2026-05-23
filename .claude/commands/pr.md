---
description: 팀 git-convention과 PR 템플릿에 맞춰 PR 본문 작성 후 gh로 PR 생성
---

# /pr

debate-tracker-backend 팀 컨벤션(`docs/git-convention.md`, `.github/pull_request_template.md`)에 맞춰 PR을 생성한다.

## 입력

- `$ARGUMENTS` (선택): 사용자가 강조하고 싶은 내용. 비어 있으면 커밋/diff/이슈에서 추론.

## 동작 흐름

### 1. 브랜치/상태 파악

병렬로 실행:

```
git branch --show-current
git rev-parse --abbrev-ref --symbolic-full-name '@{u}'
git status
git log develop..HEAD --oneline
git diff develop...HEAD --stat
```

- base 브랜치는 기본 `develop` (팀 default 브랜치). `main`으로 PR이 필요한 상황(`develop ↔ main`)은 사용자가 명시했을 때만.
- upstream이 없거나 로컬이 앞서 있으면 push 단계에서 처리.

### 2. 제목 결정 (하이브리드)

순서대로 시도:

1. **브랜치명에서 prefix 추출**
   - `feat/#10`, `refactor/#23`, `hotfix/#4` → prefix는 브랜치명 첫 segment.
   - `hotfix/*`라면 base를 `main`으로 추정 (사용자에게 재확인).

2. **이슈 타이틀 우선**
   - 브랜치명에서 `#숫자`를 추출.
   - `gh issue view <번호> --json title,body` 로 이슈 정보 가져옴.
   - 성공하면 → 제목 = `<prefix>: <이슈 타이틀에서 [FEAT]/[BUG] 같은 헤더 제거한 본문>`.

3. **이슈 없으면 커밋/diff 종합**
   - `git log develop..HEAD --oneline` 으로 커밋 목록 확인.
   - 커밋 1개면 그 커밋 메시지 제목 사용.
   - 여러 개면 Claude가 diff와 커밋을 종합해 한국어 제목 작성.

4. **사용자 confirm** 단계에서 수정 가능.

제목 형식 규칙:
- `<prefix>: <한국어 설명>` (커밋과 동일)
- 70자 이내
- 마침표 없음, 명사형 종결

### 3. 본문 작성

기존 `.github/pull_request_template.md`의 구조를 **유지**하고, 그 사이에 `## ✨ 변경사항` 섹션을 끼워 넣는다.

```markdown
# 🚩 연관 이슈
closed #<번호>

## ✨ 변경사항

- <커밋/diff에서 추출한 핵심 변경 1>
- <핵심 변경 2>
- <핵심 변경 3>

# 🗣️ 리뷰 요구사항 (선택)

- <자동 제안된 리뷰 포인트 후보 1>
- <후보 2>
```

**변경사항 작성 규칙:**
- 커밋 단위가 아닌 **사용자 관점의 변경** 위주 (예: "POST /api/sessions 추가" o, "SessionService.java 수정" x).
- 3~7 불릿. 그보다 적으면 그냥 한 줄.
- 모듈/도메인 영향 범위가 여러 곳이면 그루핑 (`app-debate:`, `infra-stt:` 같은 라벨).

**리뷰 요구사항 후보 자동 제안 기준:**
다음에 해당하는 변경이 있으면 후보로 자동 작성. 사용자가 수정/삭제 가능.
- DB 마이그레이션 (Flyway 스크립트) 추가/수정
- 외부 API 호출 추가 (`SttClient`, `LlmClient` 같은 `infra-*` 사용)
- 동시성/트랜잭션 변경 (`@Transactional`, 락, WebSocket broadcast)
- 새 의존성(`build.gradle*`) 추가
- 보안 관련 (인증/인가, 입력 검증)
- 모듈 간 의존성 변경 (`app-debate` ↔ `infra-*`)

위 후보가 없으면 리뷰 요구사항 섹션은 비워둔다(템플릿의 "(선택)" 표시 의도 존중).

### 4. 연관 이슈 처리

- 브랜치명에서 `#숫자` 추출 → `closed #<번호>`.
- 여러 이슈를 닫는 경우 사용자가 알려주면 `closed #10, closed #11`.
- 이슈가 없으면 `closed #` 부분을 그대로 두지 말고 사용자에게 물어본 후 처리. 정말 이슈 없는 PR이면 그 줄 자체를 제거.

### 5. push 처리

```
git status -sb 로 ahead/behind 확인
```

- upstream 없음 → `git push -u origin <branch>` 필요.
- 로컬이 앞서 있음 → `git push`.
- behind면 사용자에게 알림 후 중단 (자동 rebase/merge는 위험).

### 6. 사용자 confirm (필수)

다음을 모두 보여주고 진행 여부를 묻는다.

```
[Base]   develop
[Head]   feat/#10
[Push]   origin/feat/#10 에 push 필요

[제목]
feat: 토론 세션 생성 API 구현

[본문]
# 🚩 연관 이슈
closed #10

## ✨ 변경사항

- POST /api/sessions 엔드포인트 추가
- 세션 lifecycle 상태 머신 도입 (READY → IN_PROGRESS → CLOSED)
- 동시 세션 5개 제한

# 🗣️ 리뷰 요구사항 (선택)

- 세션 상태 전이 검증 위치(Entity vs Service)에 대한 의견

진행할까요? (y/n / 수정 요청)
```

수정 요청 시 해당 항목만 다시 작성.

### 7. PR 생성 실행

confirm 받으면:

```
git push -u origin <branch>   # 필요 시
gh pr create \
  --base develop \
  --head <current-branch> \
  --title "<제목>" \
  --body "$(cat <<'EOF'
<본문>
EOF
)"
```

생성 후 PR URL을 출력한다.

### 8. 후속 안내

다음 사항은 PR 생성 후 사용자에게 안내(자동 처리 X — 사람 판단 영역):

- Reviewers 할당
- Label 부여 (라벨순서: 파트 - 워크플로우)
- Project 할당
- Milestone (이슈에서 상속될 수 있음)

자동화 가능한 것(`/noti`로 디스코드 공지)이 있으면 마지막에 한 줄 알려준다.

## 머지 방식 안내

PR이 생성된 후, base에 따라 머지 방식을 한 줄로 안내:

| base ← head | 방식 |
|---|---|
| `develop ← feat/*` | Squash and merge |
| `main ← hotfix/*` | Squash and merge |
| `main ← develop` | Merge commit |
| 그 외 | Merge commit |

## 절대 금지

- `--force` push (사용자가 명시적으로 요청해도 main/develop 대상이면 거부)
- 영어 PR 제목/본문 (커밋과 동일)
- 본문에 Co-Authored-By, Generated with 같은 attribution 추가
- 리뷰 요구사항을 무리하게 채우기 (선택 항목임)
- base 브랜치를 임의로 `main`으로 지정 (hotfix 명시일 때만)
