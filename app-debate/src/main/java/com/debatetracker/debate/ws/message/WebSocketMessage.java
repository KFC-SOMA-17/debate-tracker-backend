package com.debatetracker.debate.ws.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 서버 → 클라이언트로 전송하는 모든 메시지의 공통 구조. {debateId, type, data} 로 직렬화된다.
 * 각 메시지 타입은 구체 하위 클래스로 선언하며, 자신의 정적 팩토리 메서드를 가진다.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class WebSocketMessage {

    private final long debateId;
    private final MessageType type;

    @JsonProperty("debateId")
    public final long debateId() {
        return debateId;
    }

    @JsonProperty("type")
    public final MessageType type() {
        return type;
    }

    @JsonProperty("data")
    public abstract Object data();
}
