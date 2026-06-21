package com.debatetracker.infra.stt.logger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SttVendor {

    AZURE("azure"),
    UNKNOWN("unknown");

    private final String value;
}
