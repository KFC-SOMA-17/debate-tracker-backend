package com.debatetracker.debate.log;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebateLogger {

    private final MeterRegistry meterRegistry;

    public void recordStart() {
        meterRegistry.counter("debate.start.count").increment();
    }

    public void recordStop() {
        meterRegistry.counter("debate.stop.count").increment();
    }

    public void recordOrphan() {
        meterRegistry.counter("debate.orphan.count").increment();
    }
}
