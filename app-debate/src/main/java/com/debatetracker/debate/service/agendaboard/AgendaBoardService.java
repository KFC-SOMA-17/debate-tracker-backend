package com.debatetracker.debate.service.agendaboard;

import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.repository.AgendaBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgendaBoardService {

    private final AgendaBoardRepository agendaBoardRepository;

    public AgendaBoard findByDebateId(long debateId) {
        return agendaBoardRepository.findByDebateId(debateId);
    }
}
