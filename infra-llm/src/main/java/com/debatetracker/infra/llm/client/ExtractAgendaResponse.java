package com.debatetracker.infra.llm.client;

import java.util.List;

public record ExtractAgendaResponse(
        List<ExtractAgenda> agendas
) {
}
