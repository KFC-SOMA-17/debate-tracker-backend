package com.debatetracker.debate.domain.agendaboard.repository;

import com.debatetracker.debate.domain.agendaboard.AgendaBoard;

public interface AgendaBoardRepository {

    AgendaBoard findByDebateId(long debateId);

    AgendaBoard upsert(AgendaBoard agendaBoard);
}
