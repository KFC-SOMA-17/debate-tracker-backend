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

/**
 * SpeechBox 저장 어댑터. 단건 조회·갱신은 JPA, 신규 다건 적재는 JDBC bulk insert 로 합성한다.
 */
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
    public void update(SpeechBox box) {
        speechBoxJpaRepository.save(new SpeechBoxEntity(box));
    }

    @Override
    public void saveAll(List<SpeechBox> boxes) {
        speechBoxJdbcRepository.batchInsert(boxes);
    }
}
