package com.debatetracker.infra.stt.router;

import com.debatetracker.infra.stt.logger.SttVendor;

public interface SttSession {

    SttVendor getVendor();

    boolean sendAudio(byte[] pcmData);

    void stop();

    boolean isConnected();
}
