package com.debatetracker.debate.service.debate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.service.BaseServiceTest;
import com.debatetracker.debate.ws.message.ControlMessage;
import com.debatetracker.debate.ws.message.ControlMessageType;
import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.infra.stt.client.SttClient;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
            WebSocketMessage actual = debateStreamingService.handleControlMessage(
                    new ControlMessage(ControlMessageType.START, "1"), session);

            verify(sttClient).startStreaming("1");
            assertAll(
                    () -> assertThat(actual.type()).isEqualTo(MessageType.DEBATE_START),
                    () -> assertThat(actual.debateId()).isEqualTo(1L),
                    () -> assertThat(attributes).containsEntry("debateId", "1"),
                    () -> assertThat(sessionRepository.existsByDebateId("1")).isTrue()
            );
        }

        @Test
        void STOP을_받으면_전사를_종료하고_세션을_삭제하며_DEBATE_END를_반환한다() {
            debateStreamingService.handleControlMessage(
                    new ControlMessage(ControlMessageType.START, "2"), session);

            WebSocketMessage actual = debateStreamingService.handleControlMessage(
                    new ControlMessage(ControlMessageType.STOP, "2"), session);

            verify(sttClient).stopStreaming("2");
            assertAll(
                    () -> assertThat(actual.type()).isEqualTo(MessageType.DEBATE_END),
                    () -> assertThat(actual.debateId()).isEqualTo(2L),
                    () -> assertThat(sessionRepository.existsByDebateId("2")).isFalse()
            );
        }
    }

    @Nested
    class StopDebateIfActive {

        @Test
        void 활성_세션이_없으면_전사를_종료하지_않는다() {
            attributes.put("debateId", "404");

            debateStreamingService.stopDebateIfActive(session);

            verify(sttClient, never()).stopStreaming("404");
        }

        @Test
        void debateId_attribute가_없으면_아무것도_하지_않는다() {
            debateStreamingService.stopDebateIfActive(session);

            verify(sttClient, never()).stopStreaming(Mockito.anyString());
        }
    }

    @Nested
    class SendAudioChunk {

        @Test
        void 오디오_청크를_받으면_해당_debateId로_STT에_전달한다() {
            byte[] pcm = {1, 2, 3, 4};

            debateStreamingService.sendAudioChunk("1", pcm);

            verify(sttClient).sendAudioChunk("1", pcm);
        }
    }
}
