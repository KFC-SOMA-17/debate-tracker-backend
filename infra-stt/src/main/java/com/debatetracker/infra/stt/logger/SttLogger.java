package com.debatetracker.infra.stt.logger;

import com.debatetracker.infra.stt.router.SttSessionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SttLogger {

    private static final String TAG_VENDOR = "vendor";

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer.Sample> sessionTimers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSpeakers = new ConcurrentHashMap<>();

    public SttLogger(MeterRegistry meterRegistry, SttSessionRepository sessionRepository) {
        this.meterRegistry = meterRegistry;
        enrollRecordingActiveSession(meterRegistry, sessionRepository);
    }

    private static void enrollRecordingActiveSession(
        MeterRegistry meterRegistry,
        SttSessionRepository sessionRepository) {

        Gauge.builder("stt.session.active_count", sessionRepository, repo -> repo.count())
            .tag(TAG_VENDOR, SttVendor.AZURE.getValue())
            .description("현재 활성 STT 세션 수")
            .register(meterRegistry);
    }

    // STT Session
    public void recordSessionStarted(SttVendor vendor, String sessionId) {
        meterRegistry.counter("stt.session.started", TAG_VENDOR, vendor.getValue()).increment();
        sessionTimers.put(sessionId, Timer.start(meterRegistry));
        sessionSpeakers.put(sessionId, ConcurrentHashMap.newKeySet());
    }

    public void recordSessionStopped(SttVendor vendor, String sessionId) {
        meterRegistry.counter("stt.session.stopped",
            TAG_VENDOR, vendor.getValue(), "status", "success"
        ).increment();
        stopSessionTimer(vendor, sessionId, "success");
        recordAndCleanSpeakers(vendor, sessionId);
    }

    public void recordSessionCanceled(SttVendor vendor, String sessionId, String errorCode) {
        meterRegistry.counter("stt.session.canceled",
            TAG_VENDOR, vendor.getValue(), "error_code", errorCode
        ).increment();
        stopSessionTimer(vendor, sessionId, "error");
        cleanSpeakers(sessionId);
    }

    // Audio Input
    public void recordAudioChunkSent(SttVendor vendor, SttAudioChunkStatus status) {
        meterRegistry.counter("stt.audio.chunk.sent",
            TAG_VENDOR, vendor.getValue(), "status", status.getValue()
        ).increment();
    }

    public void recordAudioBytesSent(SttVendor vendor, int bytes) {
        meterRegistry.counter("stt.audio.bytes.total", TAG_VENDOR, vendor.getValue())
            .increment(bytes);
    }

    public void recordAudioSendError(SttVendor vendor, Exception exception) {
        meterRegistry.counter("stt.audio.send.error",
            TAG_VENDOR, vendor.getValue(), "error_type", exception.getClass().getSimpleName()
        ).increment();
    }

    // Connection
    public void recordConnectionFailed(SttVendor vendor, Exception exception) {
        meterRegistry.counter("stt.connection.failed",
            TAG_VENDOR, vendor.getValue(), "error_type", exception.getClass().getSimpleName()
        ).increment();
    }

    public void recordConnectionTimeout(SttVendor vendor) {
        meterRegistry.counter("stt.connection.timeout", TAG_VENDOR, vendor.getValue()).increment();
    }

    public void recordTranscriptionSegment(SttVendor vendor, SttTranscriptionStatus status,
        String sessionId, String speakerId) {
        meterRegistry.counter("stt.transcription.segment.received",
            TAG_VENDOR, vendor.getValue(), "status", status.getValue()
        ).increment();
        if (status == SttTranscriptionStatus.SUCCESS && speakerId != null) {
            sessionSpeakers.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(speakerId);
        }
    }

    public void recordTranscriptionNoMatch(SttVendor vendor) {
        meterRegistry.counter("stt.transcription.no_match", TAG_VENDOR, vendor.getValue())
            .increment();
    }

    private void stopSessionTimer(SttVendor vendor, String sessionId, String status) {
        Timer.Sample sample = sessionTimers.remove(sessionId);
        if (sample != null) {
            sample.stop(meterRegistry.timer("stt.session.duration",
                TAG_VENDOR, vendor.getValue(), "status", status
            ));
        }
    }

    private void recordAndCleanSpeakers(SttVendor vendor, String sessionId) {
        Set<String> speakers = sessionSpeakers.remove(sessionId);
        if (speakers != null) {
            meterRegistry.summary("stt.transcription.speaker.unique", TAG_VENDOR, vendor.getValue())
                .record(speakers.size());
        }
    }

    private void cleanSpeakers(String sessionId) {
        sessionSpeakers.remove(sessionId);
    }
}
