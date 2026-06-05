package com.debatetracker.debate.service.agendaboard;

import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import java.util.List;

public interface DebateAgendaAnalyzer {

    AgendaBoard analyze(long debateId, List<SpeechBox> speeches, AgendaBoard beforeBoard);
}
