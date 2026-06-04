package com.debatetracker.infra.llm.client;

import java.util.List;

public record ExtractAgendaRequest(
        String sessionId,
        List<TranscriptSegment> contexts,
        List<ExtractAgenda> beforeAgendas
) {

}
