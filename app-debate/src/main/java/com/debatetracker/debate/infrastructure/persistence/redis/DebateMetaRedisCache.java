package com.debatetracker.debate.infrastructure.persistence.redis;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.infrastructure.config.AsyncConfig;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

/**
 * Debate 메타정보의 Redis 캐시 구현. 토론별 메타정보를 Hash 자료구조(debates:{id}:meta)로 저장하고
 * 24시간 TTL 을 건다. 중간 합류 viewer 의 초기 화면 로드(NFR RE-02)를 빠르게 하기 위한 부가 캐시다.
 *
 * <p>캐시는 부가물이므로 best-effort 로 동작한다. Redis 쓰기가 실패해도 예외를 전파하지 않고
 * 로그만 남긴다 — DebateDomainRepository 의 DB 저장 결과는 그대로 유지된다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DebateMetaRedisCache {

    public static final String META_KEY_FORMAT = "debate:%s:meta";
    public static final String TOPIC_FIELD = "topic";
    private static final Duration META_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public void save(Debate debate) {
        String metaKey = metaKey(debate.getId());
        try {
            redisTemplate.opsForHash().put(metaKey, TOPIC_FIELD, debate.getTopic());
            redisTemplate.expire(metaKey, META_TTL);
        } catch (RuntimeException e) {
            log.error("Debate 메타 캐시 저장 실패: debateId={}", debate.getId(), e);
        }
    }

    /**
     * {@link #save} 를 별도 스레드 풀({@link AsyncConfig#CACHE_EXECUTOR})에 위임하는 fire-and-forget 변형.
     * 캐시 미스 후 DB 조회 결과 재적재처럼, 호출자 응답 경로를 Redis 쓰기로 막지 않으려는 용도다.
     * (프록시 기반 @Async 라 반드시 다른 빈에서 호출되어야 한다 — self-invocation 금지.)
     */
    @Async(AsyncConfig.CACHE_EXECUTOR)
    public void saveAsync(Debate debate) {
        save(debate);
    }

    /**
     * 캐시에 메타정보가 있으면 id 와 조합해 Debate 를 만들어 반환한다. 없거나 Redis 오류면 empty 를 반환해
     * 호출 측이 DB 조회로 폴백하게 한다.
     */
    public Optional<Debate> find(Long id) {
        String metaKey = metaKey(id);
        try {
            Object topic = redisTemplate.opsForHash().get(metaKey, TOPIC_FIELD);
            if (topic == null) {
                return Optional.empty();
            }
            return Optional.of(new Debate(id, topic.toString()));
        } catch (RuntimeException e) {
            log.error("Debate 메타 캐시 조회 실패: debateId={}", id, e);
            return Optional.empty();
        }
    }

    private String metaKey(Long id) {
        return META_KEY_FORMAT.formatted(id);
    }
}
