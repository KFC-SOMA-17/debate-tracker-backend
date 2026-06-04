package com.debatetracker.debate.scheduler;

import com.debatetracker.debate.service.agendaboard.AgendaAnalyzeService;
import com.debatetracker.debate.service.debate.DebateStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgendaAnalyzeScheduler {

    private static final long ANALYZE_INTERVAL_MS = 60_000L;

    private final DebateStreamingService streamingService;
    private final AgendaAnalyzeService analyzeService;

    @Scheduled(fixedRate = ANALYZE_INTERVAL_MS)
    public void analyzeActiveSessions() {
        streamingService.findActiveSessions()
                .forEach(analyzeService::analyzeSession);
    }
}
