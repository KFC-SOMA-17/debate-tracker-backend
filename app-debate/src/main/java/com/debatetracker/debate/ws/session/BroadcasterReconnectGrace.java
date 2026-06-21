package com.debatetracker.debate.ws.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BroadcasterReconnectGrace {

    private static final Duration RECONNECT_GRACE = Duration.ofSeconds(40);

    private final ConcurrentHashMap<String, ScheduledFuture<?>> pendingTerminations = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler;

    public void scheduleTermination(String debateId, Runnable termination) {
        ScheduledFuture<?> scheduled = taskScheduler.schedule(
                () -> runTermination(debateId, termination),
                Instant.now().plus(RECONNECT_GRACE));
        cancelFuture(pendingTerminations.put(debateId, scheduled));
    }

    public void cancel(String debateId) {
        cancelFuture(pendingTerminations.remove(debateId));
    }

    private void runTermination(String debateId, Runnable termination) {
        pendingTerminations.remove(debateId);
        termination.run();
    }

    private void cancelFuture(ScheduledFuture<?> future) {
        Optional.ofNullable(future)
                .ifPresent(scheduled -> scheduled.cancel(false));
    }
}
