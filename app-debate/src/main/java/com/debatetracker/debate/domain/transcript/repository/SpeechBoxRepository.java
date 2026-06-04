package com.debatetracker.debate.domain.transcript.repository;

import com.debatetracker.debate.domain.transcript.SpeechBox;
import java.util.List;
import java.util.Optional;

/**
 * 토론별 SpeechBox 영속화 포트. 화자 단위로 조합된 SpeechBox 를 저장/갱신한다.
 *
 * <p>{@link #findLastByDebateId} 로 마지막 박스를 조회해 이어붙임(update) 대상인지 판단하고,
 * 새로 만들어진 박스들은 {@link #saveAll} 로 한 번에 적재한다. (조합 규칙은 API_DOCS §11.)
 */
public interface SpeechBoxRepository {

    Optional<SpeechBox> findLastByDebateId(long debateId);

    void update(SpeechBox box);

    void saveAll(List<SpeechBox> boxes);
}
