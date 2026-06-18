package com.debatetracker.debate.log;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StompMessageType {

    START("start"),
    STOP("stop"),
    AUDIO("audio");

    private final String value;
}
