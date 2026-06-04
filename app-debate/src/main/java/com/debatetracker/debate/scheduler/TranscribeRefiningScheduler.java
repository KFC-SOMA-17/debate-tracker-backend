package com.debatetracker.debate.scheduler;

import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.service.transcript.TranscribeRefiningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 전사 보정 트리거. 30초마다 활성 토론을 돌며 세션별 보정을 호출한다.
 *
 * <p>보정 로직 자체는 {@link TranscribeRefiningService} 가 가지며, 여기서는 주기/순회만 책임진다.
 * 세션별 보정이 실패해도 서비스가 예외를 삼키고 로그만 남기므로, 한 세션의 실패가 순회를 멈추지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscribeRefiningScheduler {

    private static final long REFINE_INTERVAL_MS = 30_000L;

    private final DebateStreamingService streamingService;
    private final TranscribeRefiningService refiningService;

    @Scheduled(fixedRate = REFINE_INTERVAL_MS)
    public void refineActiveSessions() {
        streamingService.findActiveSessions()
                .forEach(refiningService::refineSession);
    }
}
