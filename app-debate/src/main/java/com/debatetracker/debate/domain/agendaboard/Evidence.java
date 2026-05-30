package com.debatetracker.debate.domain.agendaboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Evidence {

    private final Long id;
    private final long claimId;
    private final String content;
    private final EvidenceType type;
}
