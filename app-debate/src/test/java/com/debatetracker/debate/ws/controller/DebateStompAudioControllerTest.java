package com.debatetracker.debate.ws.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.debatetracker.debate.BaseStompTest;
import com.debatetracker.debate.MessageFrameHandler;
import com.debatetracker.debate.ws.StompMessageResponse;
import com.debatetracker.infra.stt.client.SttClient;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class DebateStompAudioControllerTest extends BaseStompTest {

    @MockitoBean
    private SttClient sttClient;

    @Nested
    class SendAudio {

        @Test
        void 바이너리_오디오를_보내면_STT로_소비되고_토픽으로_broadcast되지_않는다() {
            String debateId = "1";
            byte[] pcm = {1, 2, 3, 4, 5};
            MessageFrameHandler<StompMessageResponse> handler = new MessageFrameHandler<>(StompMessageResponse.class);

            stompSession.subscribe("/topic/debate/" + debateId, handler);
            stompSession.send("/app/debate/" + debateId + "/audio", pcm);

            assertAll(
                    () -> verify(sttClient, timeout(3_000L)).sendAudioChunk(eq(debateId), any(byte[].class)),
                    () -> assertThatThrownBy(() -> handler.getCompletableFuture().get(500L, TimeUnit.MILLISECONDS))
                            .isInstanceOf(TimeoutException.class)
            );
        }
    }
}
