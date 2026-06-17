package com.debatetracker.debate.ws.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.willThrow;

import com.debatetracker.debate.BaseStompTest;
import com.debatetracker.debate.MessageFrameHandler;
import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.ws.StompMessageResponse;
import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class DebateStompErrorControllerTest extends BaseStompTest {

    @MockitoBean
    private DebateStreamingService debateStreamingService;

    @Nested
    class HandleException {

        @Test
        void start_처리중_예외가_발생하면_ERROR를_토픽으로_broadcast한다() throws Exception {
            String debateId = "1";
            long debateIdValue = 1L;
            willThrow(new DebateTrackerException(ErrorCode.INTERNAL_SERVER_ERROR))
                    .given(debateStreamingService).startDebate(debateId);
            MessageFrameHandler<StompMessageResponse> handler = new MessageFrameHandler<>(StompMessageResponse.class);

            stompSession.subscribe("/topic/debate/" + debateId, handler);
            stompSession.send("/app/debate/" + debateId + "/start", new byte[0]);

            StompMessageResponse response = handler.getCompletableFuture().get(3L, TimeUnit.SECONDS);
            assertAll(
                    () -> assertThat(response.type()).isEqualTo(MessageType.ERROR),
                    () -> assertThat(response.debateId()).isEqualTo(debateIdValue)
            );
        }
    }
}
