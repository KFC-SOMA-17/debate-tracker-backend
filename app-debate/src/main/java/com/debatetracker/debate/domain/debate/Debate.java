package com.debatetracker.debate.domain.debate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Debate {

    private final Long id;
    private final String topic;
}
