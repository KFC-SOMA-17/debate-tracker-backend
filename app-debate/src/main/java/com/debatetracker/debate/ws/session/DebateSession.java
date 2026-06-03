package com.debatetracker.debate.ws.session;

import org.springframework.web.socket.WebSocketSession;

/**
 * 진행 중인 토론의 라이브 WebSocket 연결을 들고 있는 세션 객체.
 * infra-stt 의 AzureSession 아날로그 — debateId(= 클라이언트 sessionId)를 key 로 관리한다.
 */
public record DebateSession(
        String debateId,
        WebSocketSession connection
) {

}
