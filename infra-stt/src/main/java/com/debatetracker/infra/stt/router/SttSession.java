package com.debatetracker.infra.stt.router;

public interface SttSession {

    void sendAudio(byte[] pcmData);

    void stop();

    boolean isConnected();
}
