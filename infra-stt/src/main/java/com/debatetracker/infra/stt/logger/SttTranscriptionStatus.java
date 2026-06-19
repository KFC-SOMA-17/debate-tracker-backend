package com.debatetracker.infra.stt.logger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SttTranscriptionStatus {

    SUCCESS("success"),
    EMPTY("empty");

    private final String value;
}
