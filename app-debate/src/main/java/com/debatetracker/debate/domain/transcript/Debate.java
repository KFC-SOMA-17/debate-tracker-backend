package com.debatetracker.debate.domain.transcript;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Debate {

    private final Long id;
    private final String topic;
}
