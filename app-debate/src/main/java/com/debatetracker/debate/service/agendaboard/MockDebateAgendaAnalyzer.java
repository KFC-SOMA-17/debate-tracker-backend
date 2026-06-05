package com.debatetracker.debate.service.agendaboard;

import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "llm.mode", havingValue = "mock", matchIfMissing = true)
public class MockDebateAgendaAnalyzer implements DebateAgendaAnalyzer {

    @Override
    public AgendaBoard analyze(long debateId, List<SpeechBox> speeches, AgendaBoard beforeBoard) {
        log.debug("Mock 쟁점 추출: debateId={}, speeches={}건", debateId, speeches.size());
        return beforeBoard;
    }
}
