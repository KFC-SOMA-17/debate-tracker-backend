package com.debatetracker.debate.scheduler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.service.agendaboard.AgendaAnalyzeService;
import com.debatetracker.debate.service.debate.DebateStreamingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class AgendaAnalyzeSchedulerTest {

    private DebateStreamingService streamingService;
    private AgendaAnalyzeService analyzeService;
    private AgendaAnalyzeScheduler scheduler;

    @BeforeEach
    void setUp() {
        streamingService = mock(DebateStreamingService.class);
        analyzeService = mock(AgendaAnalyzeService.class);
        scheduler = new AgendaAnalyzeScheduler(streamingService, analyzeService);
    }

    @Nested
    class AnalyzeActiveSessions {

        @Test
        void 활성_세션마다_분석을_위임_호출한다() {
            DebateSession first = new DebateSession("1", mock(WebSocketSession.class));
            DebateSession second = new DebateSession("2", mock(WebSocketSession.class));
            when(streamingService.findActiveSessions()).thenReturn(List.of(first, second));

            scheduler.analyzeActiveSessions();

            assertAll(
                    () -> verify(analyzeService).analyzeSession(first),
                    () -> verify(analyzeService).analyzeSession(second)
            );
        }

        @Test
        void 활성_세션이_없으면_아무_것도_호출하지_않는다() {
            when(streamingService.findActiveSessions()).thenReturn(List.of());

            scheduler.analyzeActiveSessions();

            verify(analyzeService, never()).analyzeSession(any());
        }
    }
}
