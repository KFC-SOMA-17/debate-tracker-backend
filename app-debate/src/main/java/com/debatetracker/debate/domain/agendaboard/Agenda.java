package com.debatetracker.debate.domain.agendaboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Agenda {

    private final Long id;
    private final long debateId;
    private final String content;
}
