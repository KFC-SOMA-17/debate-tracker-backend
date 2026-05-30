---
description: 팀 git-convention에 맞춰 한국어 커밋 메시지 작성 후 커밋
---

# /commit

debate-tracker-backend 팀 컨벤션(`.claude/conventions/git-convention.md`)에 맞춰 커밋 메시지를 작성하고 커밋을 만든다.

## 입력

- `$ARGUMENTS` (선택): 사용자가 강조하고 싶은 의도(예: "WebSocket broadcast 핵심 리팩토링"). 비어 있으면 diff에서 추론.

## 동작 흐름

### 1. 현재 상태 파악

병렬로 실행:

```
git status
git diff --staged --stat
git diff --stat
git log -5 --oneline
```

### 2. 스테이징 제안

`git status`를 보고 다음 중 하나를 선택해서 사용자에게 안내한다.

- **이미 staged 파일이 있고, unstaged는 없음** → 그대로 진행.
- **staged + unstaged 혼재** → staged만 커밋할지, 추가로 unstaged도 포함할지 사용자에게 물어본다.
- **staged 없고 unstaged만 있음** → 변경 파일 목록을 보여주고 어떤 파일을 스테이징할지 사용자에게 확인한다. 단일 의도로 묶이지 않으면 "이건 다른 커밋으로 분리하는 게 좋습니다"라고 제안.

**자동 제외 권장 파일** (사용자에게 명시적으로 확인):
- `.env`, `application-*.yml` 중 secret 가능성 있는 것
- `*.iml`, `.idea/`, `.vscode/` 등 IDE 파일
- `build/`, `out/`, `.gradle/` 등 산출물

### 3. prefix 추론

| prefix | 판단 기준 |
|---|---|
| `feat` | `src/main/**` 아래 새 파일 추가 또는 새 메서드/엔드포인트 |
| `fix` | 버그 수정(보통 `src/main/**` 수정 + 관련 test 수정). 커밋 의도가 "잘못된 동작 정정"이면 fix |
| `refactor` | `src/main/**` 변경이 있으나 동작 무변경(메서드/필드명 변경, 추출, 이동) |
| `test` | `src/test/**`만 변경 |
| `docs` | `*.md`, `docs/**`만 변경 |
| `chore` | 파일 이동/삭제/이름변경, `.github/**`, `.gitignore`, 디렉토리 구조 변경 |
| `style` | 포맷팅, 공백, 어노테이션 순서 등 컨벤션 적용만 |
| `config` | `build.gradle*`, `settings.gradle*`, `gradle/`, 의존성 추가/변경 |
| `design` | UI 관련 (백엔드 레포에선 거의 사용 안 됨) |

여러 prefix가 섞이면 (예: feat + test) **분리 커밋**을 우선 제안한다. 의도적으로 묶고 싶다는 사용자 답을 받으면 가장 핵심적인 변경의 prefix 하나로 통일하고 body에 부가 변경을 적는다.

### 4. 제목 작성

형식: `<prefix>: <한국어 설명>`

- 최근 5개 커밋(`git log -5 --oneline`)의 톤과 일치시킬 것. 예: 명사형 종결("백엔드 프로젝트 환경 설정"), 마침표 없음.
- 50자 내 권장. 70자 넘으면 줄일 것.
- 영어 메시지 작성 금지(전체 커밋 히스토리가 한국어).

### 5. body 자동 판단

다음 중 하나라도 해당하면 body 작성:

- 변경된 파일이 **3개 이상**
- 변경 라인 수가 `git diff --shortstat` 기준 **추가+삭제 ≥ 100**
- 한 커밋에 의도적으로 두 가지 이상의 의도가 묶여 있음(분리 거부 시)

body 형식 (`.claude/conventions/git-convention.md` 예시 그대로):

```
<prefix>: <제목>

- 변경 1에 대한 구체적 설명
- 변경 2에 대한 구체적 설명
```

- 불릿 2~5개 권장.
- "어떻게 바꿨는지"가 아니라 **"왜/무엇을"** 위주.
- 의문점이 있는 부분 ("Why is...") 같은 자기설명은 적지 않음. 코드/리뷰 채널의 영역.

### 6. 사용자 confirm

다음을 보여주고 사용자 확인을 받는다:

```
[스테이징할 파일]
  M  src/main/java/.../SessionService.java
  A  src/test/java/.../SessionServiceTest.java

[커밋 메시지]
feat: 토론 세션 생성 API 구현

- POST /api/sessions 엔드포인트 추가
- 세션 라이프사이클 상태 머신 도입
- 동시 세션 5개 제한 검증

진행할까요? (y/n / 수정 요청)
```

수정 요청 시 다시 4~5단계로 돌아간다.

### 7. 커밋 실행

confirm 받으면:

```
git add <선택된 파일들>
git commit -m "$(cat <<'EOF'
<메시지>
EOF
)"
```

- HEREDOC 사용으로 줄바꿈 보존.
- `--no-verify` **절대 사용 금지** (pre-commit hook이 실패하면 원인 디버깅 후 새 커밋).
- `--amend` 사용 금지 (사용자가 명시적으로 amend 요청한 경우 제외).
- Co-Authored-By trailer 추가 금지 (팀 컨벤션 + 전역 설정).

### 8. 결과 확인

`git log -1 --stat`을 보여주고 종료.

## 거부 사례

다음의 경우 커밋하지 말고 사용자에게 알린다:

- 변경 사항이 없음
- secret 가능성 있는 파일이 staged 영역에 있음 (확인 후 해제 제안)
- 한 커밋에 너무 많은 의도가 섞여 있고 사용자가 분리 의사 명확히 함

## 절대 금지

- 영어 커밋 메시지
- `--no-verify`, `-i` 옵션
- 이전 커밋 amend (명시적 요청 시 제외)
- `git add .` / `git add -A` (의도치 않은 파일 포함 위험)
- 푸시(`git push`) - 커밋만 만들고 종료
