package com.debatetracker.debate.domain.transcript.repository;

import com.debatetracker.debate.domain.transcript.SpeechBox;
import java.util.List;
import java.util.Optional;

public interface SpeechBoxRepository {

    Optional<SpeechBox> findLastByDebateId(long debateId);

    void update(SpeechBox box);

    void saveAll(List<SpeechBox> boxes);
}
