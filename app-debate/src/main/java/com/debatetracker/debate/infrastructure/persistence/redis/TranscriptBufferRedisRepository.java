package com.debatetracker.debate.infrastructure.persistence.redis;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.infrastructure.persistence.redis.dto.RawSegmentJson;
import com.debatetracker.debate.infrastructure.persistence.redis.dto.RefinedSegmentJson;
import com.debatetracker.serdes.JsonUtils;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 전사 버퍼의 Redis 저장 구현. 토론별 raw/refined 세그먼트를 List 자료구조로 관리한다.
 *
 * <p>SpeechSegment / RefinedSpeechSegment 는 Jackson 생성자 메타가 없는 도메인 클래스라
 * 폴리모픽 직렬화가 깨지기 쉽다. 도메인 객체를 오염시키지 않도록, 직렬화 전용 record 로 매핑한 뒤 공통 {@link com.debatetracker.serdes.SerDesUtils} 로 JSON
 * 문자열을 만들어 StringRedisTemplate 에 저장한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TranscriptBufferRedisRepository {

    private static final String RAW_KEY_FORMAT = "debate:%s:raw-segments";
    private static final String REFINED_KEY_FORMAT = "debate:%s:refined-segments";

    private final StringRedisTemplate redisTemplate;

    public void appendRaw(String debateId, SpeechSegment segment) {
        String rawKey = rawKey(debateId);
        String serializedRawSegments = JsonUtils.serialize(new RawSegmentJson(segment));
        listOps().rightPush(rawKey, serializedRawSegments);
    }

    public long countRawSegments(String debateId) {
        return Optional.ofNullable(listOps().size(rawKey(debateId)))
                .orElse(0L);
    }

    public List<SpeechSegment> peekRaw(String debateId, int count) {
        return Optional.ofNullable(listOps().range(rawKey(debateId), 0, count - 1L))
                .orElseGet(List::of)
                .stream()
                .map(value -> JsonUtils.deserialize(value, RawSegmentJson.class).toDomain())
                .toList();
    }

    public void trimRaw(String debateId, int count) {
        listOps().trim(rawKey(debateId), count, -1);
    }

    public List<RefinedSpeechSegment> recentRefined(String debateId, int n) {
        return Optional.ofNullable(listOps().range(refinedKey(debateId), -n, -1))
                .orElseGet(List::of)
                .stream()
                .map(value -> JsonUtils.deserialize(value, RefinedSegmentJson.class).toDomain())
                .toList();
    }

    public void appendRefined(String debateId, List<RefinedSpeechSegment> segments) {
        if (segments.isEmpty()) {
            return;
        }
        List<String> values = segments.stream()
                .map(segment -> JsonUtils.serialize(new RefinedSegmentJson(segment)))
                .toList();
        listOps().rightPushAll(refinedKey(debateId), values);
    }

    public void clear(String debateId) {
        redisTemplate.delete(rawKey(debateId));
        redisTemplate.delete(refinedKey(debateId));
    }

    private ListOperations<String, String> listOps() {
        return redisTemplate.opsForList();
    }

    private String rawKey(String debateId) {
        return RAW_KEY_FORMAT.formatted(debateId);
    }

    private String refinedKey(String debateId) {
        return REFINED_KEY_FORMAT.formatted(debateId);
    }
}
