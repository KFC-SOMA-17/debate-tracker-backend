package com.debatetracker.infra.stt.client;

import com.debatetracker.infra.stt.client.dto.SttSegment;
import java.util.function.Consumer;

public interface SttClient {

    /**
     * 스트리밍 전사를 시작한다.
     *
     * @param sessionId 세션 식별자
     */
    void startStreaming(String sessionId);

    /**
     * 특정 세션의 스트리밍을 중지한다.
     *
     * @param sessionId 세션 식별자
     */
    void stopStreaming(String sessionId);

    /**
     * 특정 세션에 오디오 청크를 벤더에 전송한다.
     *
     * @param sessionId 세션 식별자
     * @param pcmData   PCM 16-bit LE 오디오 데이터
     */
    void sendAudioChunk(String sessionId, byte[] pcmData);

    /**
     * 특정 세션의 연결 상태를 확인한다.
     *
     * @param sessionId 세션 식별자
     */
    boolean isConnected(String sessionId);

    String getVendorName();
}
