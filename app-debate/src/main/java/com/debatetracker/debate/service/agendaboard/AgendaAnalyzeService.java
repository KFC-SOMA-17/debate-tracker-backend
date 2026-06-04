package com.debatetracker.debate.service.agendaboard;

import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.repository.AgendaBoardRepository;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import com.debatetracker.debate.domain.transcript.repository.SpeechBoxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgendaAnalyzeService {

    private final SpeechBoxRepository speechBoxRepository;
    private final AgendaBoardRepository agendaBoardRepository;
    private final DebateAgendaAnalyzer agendaAnalyzer;

    public void analyzeSession(DebateSession session) {
        long debateId = Long.parseLong(session.debateId());
        try {
            List<SpeechBox> speeches = speechBoxRepository.findAllByDebateId(debateId);
            if (speeches.isEmpty()) {
                return;
            }
            AgendaBoard beforeBoard = agendaBoardRepository.findByDebateId(debateId);
            AgendaBoard analyzed = agendaAnalyzer.analyze(debateId, speeches, beforeBoard);
            agendaBoardRepository.upsert(analyzed);
            log.info("쟁점 추출 완료: debateId={}, agendas={}건", debateId, analyzed.getAgendas().size());
        } catch (Exception e) {
            log.error("쟁점 추출 실패 — 다음 주기 재시도: debateId={}", debateId, e);
        }
    }
}
