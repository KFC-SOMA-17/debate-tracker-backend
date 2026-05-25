package com.debatetracker.debate.document;

public enum Tag {

    // 예시 작성 - 실제 API 문서화 시, 사용하는 태그만 남겨놓을 것
    SESSION_API("Session API"),
    UTTERANCE_API("Utterance API"),
    ISSUE_API("Issue API"),
    SPEAKER_API("Speaker API"),
    ;

    private final String displayName;

    Tag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
