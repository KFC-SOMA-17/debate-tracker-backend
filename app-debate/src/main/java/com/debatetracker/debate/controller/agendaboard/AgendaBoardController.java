package com.debatetracker.debate.controller.agendaboard;

import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.service.agendaboard.AgendaBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AgendaBoardController {

    private final AgendaBoardService agendaBoardService;

    @GetMapping("/api/debates/{debateId}/agendas")
    public ResponseEntity<AgendaBoardResponse> getAgendas(@PathVariable long debateId) {
        AgendaBoard agendaBoard = agendaBoardService.findByDebateId(debateId);
        return ResponseEntity.ok(AgendaBoardResponse.from(agendaBoard));
    }
}
