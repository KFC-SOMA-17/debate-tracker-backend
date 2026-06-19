package com.debatetracker.infra.stt.logger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SttAudioChunkStatus {

    SUCCESS("success"),
    NO_SESSION("no_session");

    private final String value;
}
