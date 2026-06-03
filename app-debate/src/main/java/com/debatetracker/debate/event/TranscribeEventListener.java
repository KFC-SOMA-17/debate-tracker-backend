package com.debatetracker.debate.event;

import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.debate.ws.id.SegmentIdGenerator;
import com.debatetracker.debate.ws.message.TranscriptionMessage;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.infra.stt.client.dto.SttSegment;
import com.debatetracker.infra.stt.client.event.TranscribeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * infra-stt 가 발행한 확정 전사 이벤트(TranscribeEvent)를 받아 해당 토론 연결로 TRANSCRIPTION 을 전송한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscribeEventListener {

    private final DebateSessionRepository sessionRepository;
    private final WebSocketMessageSender messageSender;
    private final SegmentIdGenerator segmentIdGenerator;

    @EventListener
    public void onTranscribe(TranscribeEvent event) {
        String debateId = event.sessionId();
        sessionRepository.findByDebateId(debateId).ifPresentOrElse(
                session -> sendTranscription(session, debateId, event.segment()),
                () -> log.debug("활성 세션 없음, 전사 결과 무시: debateId={}", debateId)
        );
    }

    private void sendTranscription(DebateSession session, String debateId, SttSegment segment) {
        SpeechSegment transcription = new SpeechSegment(
                segmentIdGenerator.generate(),
                segment.content(),
                segment.speaker(),
                segment.start(),
                segment.end()
        );
        messageSender.send(session, new TranscriptionMessage(Long.parseLong(debateId), transcription));
    }
}
