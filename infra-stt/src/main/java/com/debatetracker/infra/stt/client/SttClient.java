package com.debatetracker.infra.stt.client;

import com.debatetracker.infra.stt.dto.SttSegment;
import java.util.function.Consumer;

public interface SttClient {

    /**
     * 스트리밍 전사를 시작한다.
     *
     * @param sessionId 세션 식별자
     * @param onSegment 전사 세그먼트가 생성될 때마다 호출되는 콜백
     */
    void startStreaming(String sessionId, Consumer<SttSegment> onSegment);

    void stopStreaming();

    /**
     * 오디오 청크를 벤더에 전송한다.
     *
     * @param pcmData PCM 16-bit LE 오디오 데이터
     */
    void sendAudioChunk(byte[] pcmData);

    boolean isConnected();

    String getVendorName();
}
