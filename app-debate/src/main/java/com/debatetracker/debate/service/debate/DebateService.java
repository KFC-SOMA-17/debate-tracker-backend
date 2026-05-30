package com.debatetracker.debate.service.debate;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.domain.debate.DebateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DebateService {

    private final DebateRepository debateRepository;

    public Debate create(Debate debate) {
        return debateRepository.create(debate);
    }
}
