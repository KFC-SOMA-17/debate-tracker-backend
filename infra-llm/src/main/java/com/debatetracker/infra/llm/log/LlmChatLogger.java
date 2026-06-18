package com.debatetracker.infra.llm.log;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LlmChatLogger {

    private final MeterRegistry meterRegistry;

    public <T> T executeWithMetrics(LlmOperationType operationType, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = action.get();
            recordRequestSuccess(operationType);
            return result;
        } catch (RuntimeException exception) {
            recordRequestError(operationType, exception);
            throw exception;
        } finally {
            stopRequestTimer(operationType, sample);
        }
    }

    private void recordRequestSuccess(LlmOperationType operationType) {
        meterRegistry.counter("llm.request.count",
            "operation", operationType.getValue(),
            "status", "success"
        ).increment();
    }

    private void stopRequestTimer(LlmOperationType operationType, Timer.Sample sample) {
        sample.stop(meterRegistry.timer("llm.request.duration",
            "operation", operationType.getValue()
        ));
    }

    private void recordRequestError(LlmOperationType operationType, Exception exception) {
        meterRegistry.counter("llm.request.count",
            "operation", operationType.getValue(),
            "status", "error",
            "error_type", exception.getClass().getSimpleName()
        ).increment();
    }

    public void recordTokenUsage(LlmOperationType operationType, String model,
                                  Integer promptTokens, Integer generationTokens) {
        if (promptTokens != null) {
            meterRegistry.counter("llm.tokens.total",
                "type", "prompt",
                "operation", operationType.getValue(),
                "model", tagOrUnknown(model)
            ).increment(promptTokens);
        }

        if (generationTokens != null) {
            meterRegistry.counter("llm.tokens.total",
                "type", "generation",
                "operation", operationType.getValue(),
                "model", tagOrUnknown(model)
            ).increment(generationTokens);
        }
    }

    public void recordFinishReason(LlmOperationType operationType, String model, String finishReason) {
        meterRegistry.counter("llm.finish_reason",
            "operation", operationType.getValue(),
            "reason", tagOrUnknown(finishReason),
            "model", tagOrUnknown(model)
        ).increment();
    }

    public void recordValidationError(LlmOperationType operationType, String model, Exception exception) {
        meterRegistry.counter("llm.validation.error",
            "operation", operationType.getValue(),
            "model", tagOrUnknown(model),
            "error_type", exception.getClass().getSimpleName()
        ).increment();
    }

    private String tagOrUnknown(String value) {
        return (value == null || value.isBlank()) ? "unknown" : value;
    }
}
