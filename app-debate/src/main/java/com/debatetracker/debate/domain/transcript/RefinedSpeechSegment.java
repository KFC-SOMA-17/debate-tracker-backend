package com.debatetracker.debate.domain.transcript;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RefinedSpeechSegment {

    private final String id;
    private final String content;
    private final String speaker;
    private final BigDecimal startAt;
    private final BigDecimal endAt;
}
