package com.debatetracker.infra.stt.router;

public interface SttSessionCreator {

    SttSession create(String sessionId);
}
