package com.debatetracker.debate.infrastructure.persistence.jdbc.transcript;

import com.debatetracker.debate.domain.transcript.SpeechBox;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SpeechBoxJdbcRepository {

    private static final String INSERT_SQL = """
            INSERT INTO speech_box (debate_id, speaker, content, start_at, end_at)
            VALUES (:debateId, :speaker, :content, :startAt, :endAt)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void batchInsert(List<SpeechBox> boxes) {
        if (boxes.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = boxes.stream()
                .map(this::toParameterSource)
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(INSERT_SQL, batch);
    }

    private SqlParameterSource toParameterSource(SpeechBox box) {
        return new MapSqlParameterSource()
                .addValue("debateId", box.getDebateId())
                .addValue("speaker", box.getSpeaker())
                .addValue("content", box.getContent())
                .addValue("startAt", box.getStartAt())
                .addValue("endAt", box.getEndAt());
    }
}
