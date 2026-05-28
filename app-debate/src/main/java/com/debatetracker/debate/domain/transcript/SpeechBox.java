package com.debatetracker.debate.domain.transcript;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SpeechBox {

    private final Long id;
    private final long debateId;
    private final String speaker;
    private final String content;
    private final BigDecimal startAt;
    private final BigDecimal endAt;
}
