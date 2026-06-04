package com.debatetracker.debate.infrastructure.persistence.jdbc.agendaboard;

import com.debatetracker.debate.domain.agendaboard.Evidence;
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
public class EvidenceJdbcRepository {

    private static final String[] KEY_COLUMNS = {"id"};

    private static final String INSERT_SQL = """
            INSERT INTO evidence (claim_id, content, type, created_at, modified_at)
            VALUES (:claimId, :content, :type, :createdAt, :modifiedAt)
            """;
    private static final String UPDATE_SQL = """
            UPDATE evidence SET content = :content, type = :type, modified_at = :modifiedAt WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<Long> batchInsert(List<Evidence> evidences) {
        if (evidences.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        SqlParameterSource[] batch = evidences.stream()
                .map(evidence -> new MapSqlParameterSource()
                        .addValue("claimId", evidence.getClaimId())
                        .addValue("content", evidence.getContent())
                        .addValue("type", evidence.getType().name())
                        .addValue("createdAt", now)
                        .addValue("modifiedAt", now))
                .toArray(SqlParameterSource[]::new);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.batchUpdate(INSERT_SQL, batch, keyHolder, KEY_COLUMNS);
        return keyHolder.getKeyList().stream()
                .map(this::extractKey)
                .toList();
    }

    public void batchUpdate(List<Evidence> evidences) {
        if (evidences.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        SqlParameterSource[] batch = evidences.stream()
                .map(evidence -> new MapSqlParameterSource()
                        .addValue("id", evidence.getId())
                        .addValue("content", evidence.getContent())
                        .addValue("type", evidence.getType().name())
                        .addValue("modifiedAt", now))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPDATE_SQL, batch);
    }

    private Long extractKey(Map<String, Object> keys) {
        Object value = keys.values().iterator().next();
        return ((Number) value).longValue();
    }
}
