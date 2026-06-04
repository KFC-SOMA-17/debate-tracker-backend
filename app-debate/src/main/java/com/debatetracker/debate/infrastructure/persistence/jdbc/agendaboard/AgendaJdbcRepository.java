package com.debatetracker.debate.infrastructure.persistence.jdbc.agendaboard;

import com.debatetracker.debate.domain.agendaboard.Agenda;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AgendaJdbcRepository {

    private static final String[] KEY_COLUMNS = {"id"};

    private static final String INSERT_SQL = """
            INSERT INTO agenda (debate_id, content, created_at, modified_at)
            VALUES (:debateId, :content, :createdAt, :modifiedAt)
            """;
    private static final String UPDATE_SQL = """
            UPDATE agenda SET content = :content, modified_at = :modifiedAt WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<Long> batchInsert(List<Agenda> agendas) {
        if (agendas.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        SqlParameterSource[] batch = agendas.stream()
                .map(agenda -> new MapSqlParameterSource()
                        .addValue("debateId", agenda.getDebateId())
                        .addValue("content", agenda.getContent())
                        .addValue("createdAt", now)
                        .addValue("modifiedAt", now))
                .toArray(SqlParameterSource[]::new);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.batchUpdate(INSERT_SQL, batch, keyHolder, KEY_COLUMNS);
        return keyHolder.getKeyList().stream()
                .map(this::extractKey)
                .toList();
    }

    public void batchUpdate(List<Agenda> agendas) {
        if (agendas.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        SqlParameterSource[] batch = agendas.stream()
                .map(agenda -> new MapSqlParameterSource()
                        .addValue("id", agenda.getId())
                        .addValue("content", agenda.getContent())
                        .addValue("modifiedAt", now))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPDATE_SQL, batch);
    }

    private Long extractKey(Map<String, Object> keys) {
        Object value = keys.values().iterator().next();
        return ((Number) value).longValue();
    }
}
