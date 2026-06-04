package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.domain.transcript.SpeechBox;
import com.debatetracker.debate.domain.transcript.repository.SpeechBoxRepository;
import com.debatetracker.debate.infrastructure.persistence.jdbc.transcript.SpeechBoxJdbcRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.transcript.SpeechBoxEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.transcript.SpeechBoxJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpeechBoxDomainRepository implements SpeechBoxRepository {

    private final SpeechBoxJpaRepository speechBoxJpaRepository;
    private final SpeechBoxJdbcRepository speechBoxJdbcRepository;

    @Override
    public Optional<SpeechBox> findLastByDebateId(long debateId) {
        return speechBoxJpaRepository.findFirstByDebateIdOrderByIdDesc(debateId)
                .map(SpeechBoxEntity::toDomain);
    }

    @Override
    public List<SpeechBox> findAllByDebateId(long debateId) {
        return speechBoxJpaRepository.findByDebateIdOrderByIdAsc(debateId).stream()
                .map(SpeechBoxEntity::toDomain)
                .toList();
    }

    @Override
    public void update(SpeechBox box) {
        speechBoxJpaRepository.save(new SpeechBoxEntity(box));
    }

    @Override
    public void saveAll(List<SpeechBox> boxes) {
        if (boxes.isEmpty()) {
            return;
        }
        speechBoxJdbcRepository.batchInsert(boxes);
    }
}
