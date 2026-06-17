package com.debatetracker.infra.llm.log;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LlmChatLogger {

    private final MeterRegistry meterRegistry;

    public Timer.Sample startRequestTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordRequestSuccess(String operation) {
        meterRegistry.counter("llm.request.count",
            "operation", operation,
            "status", "success"
        ).increment();
    }

    public void stopRequestTimer(String operation, Timer.Sample sample) {
        sample.stop(meterRegistry.timer("llm.request.duration",
            "operation", operation
        ));
    }

    public void recordRequestError(String operation, Exception exception) {
        meterRegistry.counter("llm.request.count",
            "operation", operation,
            "status", "error",
            "error_type", exception.getClass().getSimpleName()
        ).increment();
    }

    public void recordTokenUsage(String operation, String model,
                                  Integer promptTokens, Integer generationTokens) {
        if (promptTokens != null) {
            meterRegistry.counter("llm.tokens.total",
                "type", "prompt",
                "operation", operation,
                "model", model
            ).increment(promptTokens);
        }

        if (generationTokens != null) {
            meterRegistry.counter("llm.tokens.total",
                "type", "generation",
                "operation", operation,
                "model", model
            ).increment(generationTokens);
        }
    }

    public void recordFinishReason(String operation, String model, String finishReason) {
        meterRegistry.counter("llm.finish_reason",
            "operation", operation,
            "reason", finishReason,
            "model", model
        ).increment();
    }

    public void recordValidationError(String operation, String model, Exception exception) {
        meterRegistry.counter("llm.validation.error",
            "operation", operation,
            "model", model,
            "error_type", exception.getClass().getSimpleName()
        ).increment();
    }
}
