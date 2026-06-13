package com.debatetracker.debate.ws.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.BaseStompTest;
import com.debatetracker.debate.MessageFrameHandler;
import com.debatetracker.debate.ws.StompMessageResponse;
import com.debatetracker.debate.ws.message.MessageType;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DebateStompControllerTest extends BaseStompTest {

    @Nested
    class StartDebate {

        @Test
        void start를_보내면_DEBATE_START를_토픽으로_broadcast한다() throws Exception {
            String debateId = "1";
            long debateIdValue = 1L;
            MessageFrameHandler<StompMessageResponse> handler = new MessageFrameHandler<>(StompMessageResponse.class);

            stompSession.subscribe("/topic/debate/" + debateId, handler);
            stompSession.send("/app/debate/" + debateId + "/start", new byte[0]);

            StompMessageResponse response = handler.getCompletableFuture().get(3L, TimeUnit.SECONDS);
            assertAll(
                    () -> assertThat(response.type()).isEqualTo(MessageType.DEBATE_START),
                    () -> assertThat(response.debateId()).isEqualTo(debateIdValue)
            );
        }
    }

    @Nested
    class StopDebate {

        @Test
        void stop을_보내면_DEBATE_END를_토픽으로_broadcast한다() throws Exception {
            String debateId = "2";
            long debateIdValue = 2L;
            MessageFrameHandler<StompMessageResponse> handler = new MessageFrameHandler<>(StompMessageResponse.class);

            stompSession.subscribe("/topic/debate/" + debateId, handler);
            stompSession.send("/app/debate/" + debateId + "/stop", new byte[0]);

            StompMessageResponse response = handler.getCompletableFuture().get(3L, TimeUnit.SECONDS);
            assertAll(
                    () -> assertThat(response.type()).isEqualTo(MessageType.DEBATE_END),
                    () -> assertThat(response.debateId()).isEqualTo(debateIdValue)
            );
        }
    }
}
