package com.debatetracker.debate.scheduler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.service.transcript.TranscribeRefiningService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class TranscribeRefiningSchedulerTest {

    private DebateStreamingService streamingService;
    private TranscribeRefiningService refiningService;
    private TranscribeRefiningScheduler scheduler;

    @BeforeEach
    void setUp() {
        streamingService = mock(DebateStreamingService.class);
        refiningService = mock(TranscribeRefiningService.class);
        scheduler = new TranscribeRefiningScheduler(streamingService, refiningService);
    }

    @Nested
    class RefineActiveSessions {

        @Test
        void 활성_세션마다_보정을_위임_호출한다() {
            String firstDebateId = "1";
            String secondDebateId = "2";
            DebateSession first = new DebateSession(firstDebateId, mock(WebSocketSession.class));
            DebateSession second = new DebateSession(secondDebateId, mock(WebSocketSession.class));
            when(streamingService.findActiveSessions()).thenReturn(List.of(first, second));

            scheduler.refineActiveSessions();

            assertAll(
                    () -> verify(refiningService).refineSession(first),
                    () -> verify(refiningService).refineSession(second)
            );
        }

        @Test
        void 활성_세션이_없으면_아무_것도_호출하지_않는다() {
            when(streamingService.findActiveSessions()).thenReturn(List.of());

            scheduler.refineActiveSessions();

            verify(refiningService, never()).refineSession(any());
        }
    }
}
