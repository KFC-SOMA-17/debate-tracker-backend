package com.debatetracker.debate.infrastructure.persistence.jdbc.agendaboard;

import com.debatetracker.debate.domain.agendaboard.Claim;
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
public class ClaimJdbcRepository {

    private static final String[] KEY_COLUMNS = {"id"};

    private static final String INSERT_SQL = """
            INSERT INTO claim (agenda_id, content, stance, created_at, modified_at)
            VALUES (:agendaId, :content, :stance, :createdAt, :modifiedAt)
            """;
    private static final String UPDATE_SQL = """
            UPDATE claim SET content = :content, stance = :stance, modified_at = :modifiedAt WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<Long> batchInsert(List<Claim> claims) {
        if (claims.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        SqlParameterSource[] batch = claims.stream()
                .map(claim -> new MapSqlParameterSource()
                        .addValue("agendaId", claim.getAgendaId())
                        .addValue("content", claim.getContent())
                        .addValue("stance", claim.getStance().name())
                        .addValue("createdAt", now)
                        .addValue("modifiedAt", now))
                .toArray(SqlParameterSource[]::new);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.batchUpdate(INSERT_SQL, batch, keyHolder, KEY_COLUMNS);
        return keyHolder.getKeyList().stream()
                .map(this::extractKey)
                .toList();
    }

    public void batchUpdate(List<Claim> claims) {
        if (claims.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        SqlParameterSource[] batch = claims.stream()
                .map(claim -> new MapSqlParameterSource()
                        .addValue("id", claim.getId())
                        .addValue("content", claim.getContent())
                        .addValue("stance", claim.getStance().name())
                        .addValue("modifiedAt", now))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPDATE_SQL, batch);
    }

    private Long extractKey(Map<String, Object> keys) {
        Object value = keys.values().iterator().next();
        return ((Number) value).longValue();
    }
}
