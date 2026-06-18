package com.debatetracker.infra.llm.log;

import lombok.Getter;

@Getter
public enum LlmOperationType {
    REFINE("refine"),
    EXTRACT("extract");

    private final String value;

    LlmOperationType(String value) {
        this.value = value;
    }
}
