package com.debatetracker.debate.domain.agendaboard;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Claim {

    private final Long id;
    private final long agendaId;
    private final String content;
    private final Stance stance;
    private final LocalDateTime createdAt;
    private final LocalDateTime modifiedAt;
    private final List<Evidence> evidences;
}
