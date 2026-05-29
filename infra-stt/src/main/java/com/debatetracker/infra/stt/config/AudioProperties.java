package com.debatetracker.infra.stt.config;

/**
 * 오디오 캡처 설정 바인딩. application.yml의 stt.audio 섹션에 매핑된다.
 */
public record AudioProperties(
        int sampleRate,
        int channels,
        String encoding,
        int chunkDurationMs
) {

    /**
     * 하나의 오디오 청크 크기(바이트)를 계산한다. LINEAR16 = 16bit = 2bytes per sample
     */
    public int chunkSizeBytes() {
        int bytesPerSample = 2; // 16-bit
        return sampleRate * bytesPerSample * channels * chunkDurationMs / 1000;
    }
}
