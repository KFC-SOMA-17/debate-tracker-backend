package com.debatetracker.debate.service.debate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
import com.debatetracker.debate.service.BaseServiceTest;
import com.debatetracker.debate.service.transcript.TranscribeRefiningService;
import com.debatetracker.debate.ws.message.ControlMessage;
import com.debatetracker.debate.ws.message.ControlMessageType;
import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.infra.stt.client.SttClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketSession;

class DebateStreamingServiceTest extends BaseServiceTest {

    @Autowired
    private DebateStreamingService debateStreamingService;

    @Autowired
    private DebateSessionRepository sessionRepository;

    @MockitoBean
    private SttClient sttClient;

    @MockitoBean
    private TranscriptBufferRepository bufferRepository;

    @MockitoBean
    private TranscribeRefiningService refiningService;

    private WebSocketSession session;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        session = Mockito.mock(WebSocketSession.class);
        attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
    }

    @Nested
    class HandleControlMessage {

        @Test
        void START를_받으면_전사를_시작하고_세션을_저장하며_DEBATE_START를_반환한다() {
            String debateId = "1";
            long debateIdValue = 1L;

            WebSocketMessage actual = debateStreamingService.handleControlMessage(
                    new ControlMessage(ControlMessageType.START, debateId), session);

            assertAll(
                    () -> verify(sttClient).startStreaming(debateId),
                    () -> assertThat(actual.type()).isEqualTo(MessageType.DEBATE_START),
                    () -> assertThat(actual.debateId()).isEqualTo(debateIdValue),
                    () -> assertThat(attributes).containsEntry("debateId", debateId),
                    () -> assertThat(sessionRepository.existsByDebateId(debateId)).isTrue()
            );
        }

        @Test
        void STOP을_받으면_전사를_종료하고_세션을_삭제하며_DEBATE_END를_반환한다() {
            String debateId = "2";
            long debateIdValue = 2L;

            debateStreamingService.handleControlMessage(
                    new ControlMessage(ControlMessageType.START, debateId), session);

            WebSocketMessage actual = debateStreamingService.handleControlMessage(
                    new ControlMessage(ControlMessageType.STOP, debateId), session);

            assertAll(
                    () -> verify(sttClient).stopStreaming(debateId),
                    () -> verify(bufferRepository).clear(debateId),
                    () -> assertThat(actual.type()).isEqualTo(MessageType.DEBATE_END),
                    () -> assertThat(actual.debateId()).isEqualTo(debateIdValue),
                    () -> assertThat(sessionRepository.existsByDebateId(debateId)).isFalse()
            );
        }
    }

    @Nested
    class StopDebateWithRemainingRefine {

        @Test
        void STT_종료_세션_삭제_후_남은_raw를_보정하고_버퍼를_정리한다() {
            String debateId = "7";
            debateStreamingService.handleControlMessage(
                    new ControlMessage(ControlMessageType.START, debateId), session);

            debateStreamingService.stopDebateWithRemainingRefine(session);

            InOrder order = Mockito.inOrder(sttClient, refiningService, bufferRepository);
            assertAll(
                    () -> order.verify(sttClient).stopStreaming(debateId),
                    () -> order.verify(refiningService).refineRemaining(any(DebateSession.class)),
                    () -> order.verify(bufferRepository).clear(debateId),
                    () -> assertThat(sessionRepository.existsByDebateId(debateId)).isFalse()
            );
        }

        @Test
        void 활성_세션이_없으면_보정도_정리도_하지_않는다() {
            String debateId = "404";
            attributes.put("debateId", debateId);

            debateStreamingService.stopDebateWithRemainingRefine(session);

            assertAll(
                    () -> verify(sttClient, never()).stopStreaming(debateId),
                    () -> verify(refiningService, never()).refineRemaining(any(DebateSession.class))
            );
        }
    }

    @Nested
    class StopDebateIfActive {

        @Test
        void 활성_세션이_없으면_전사를_종료하지_않는다() {
            String debateId = "404";
            attributes.put("debateId", debateId);

            debateStreamingService.stopDebateIfActive(session);

            verify(sttClient, never()).stopStreaming(debateId);
        }

        @Test
        void debateId_attribute가_없으면_아무것도_하지_않는다() {
            debateStreamingService.stopDebateIfActive(session);

            verify(sttClient, never()).stopStreaming(Mockito.anyString());
        }

        @Test
        void 네트워크_끊김_정리는_남은_raw를_보정하지_않는다() {
            String debateId = "8";
            debateStreamingService.handleControlMessage(
                    new ControlMessage(ControlMessageType.START, debateId), session);

            debateStreamingService.stopDebateIfActive(session);

            assertAll(
                    () -> verify(sttClient).stopStreaming(debateId),
                    () -> verify(refiningService, never()).refineRemaining(any(DebateSession.class)),
                    () -> verify(bufferRepository).clear(debateId)
            );
        }
    }

    @Nested
    class SendAudioChunk {

        @Test
        void 오디오_청크를_받으면_해당_debateId로_STT에_전달한다() {
            String debateId = "1";
            byte[] pcm = {1, 2, 3, 4};

            debateStreamingService.sendAudioChunk(debateId, pcm);

            verify(sttClient).sendAudioChunk(debateId, pcm);
        }
    }

    @Nested
    class FindActiveSessions {

        @Test
        void 시작된_세션들을_활성_세션으로_반환한다() {
            String firstDebateId = "1";
            String secondDebateId = "2";
            startDebate(firstDebateId);
            startDebate(secondDebateId);

            List<DebateSession> active = debateStreamingService.findActiveSessions();

            assertThat(active)
                    .extracting(DebateSession::debateId)
                    .containsExactlyInAnyOrder(firstDebateId, secondDebateId);
        }

        @Test
        void 시작된_세션이_없으면_빈_목록을_반환한다() {
            List<DebateSession> active = debateStreamingService.findActiveSessions();

            assertThat(active).isEmpty();
        }

        private void startDebate(String debateId) {
            WebSocketSession connection = Mockito.mock(WebSocketSession.class);
            when(connection.getAttributes()).thenReturn(new HashMap<>());
            debateStreamingService.handleControlMessage(
                    new ControlMessage(ControlMessageType.START, debateId), connection);
        }
    }
}
