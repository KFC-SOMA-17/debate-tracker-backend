package com.debatetracker.debate.domain.agendaboard;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AgendaBoard {

    private final Long debateId;
    private final List<Agenda> agendas;
}
