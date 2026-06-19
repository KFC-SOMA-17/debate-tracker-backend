package com.debatetracker.infra.stt.logger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SttVendor {

    AZURE("azure");

    private final String value;
}
