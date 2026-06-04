package com.debatetracker.debate.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.fixture.DebateGenerator;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateEntity;
import com.debatetracker.debate.infrastructure.persistence.redis.DebateMetaRedisCache;
import com.debatetracker.exception.DebateTrackerException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

public class DebateDomainRepositoryTest extends BaseDomainRepositoryTest {

    @Autowired
    private DebateDomainRepository debateDomainRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DebateGenerator debateGenerator;

    // 캐시는 generator 가 아닌 각 테스트의 책임이므로, 캐시를 남긴 토론 id 를 모아 @AfterEach 에서 정리한다.
    private final List<Long> cachedDebateIds = new ArrayList<>();

    @AfterEach
    void cleanUpMetaCache() {
        cachedDebateIds.forEach(id -> redisTemplate.delete(DebateMetaRedisCache.META_KEY_FORMAT.formatted(id)));
        cachedDebateIds.clear();
    }

    @Nested
    class Create {

        @Test
        void 토론을_저장하고_식별자가_부여된_도메인을_반환한다() {
            String topic = "토론 주제";

            Debate created = debateDomainRepository.create(new Debate(null, topic));
            cachedDebateIds.add(created.getId());

            assertAll(
                    () -> assertThat(created.getId()).isNotNull(),
                    () -> assertThat(created.getTopic()).isEqualTo(topic)
            );
        }

        @Test
        void 저장한_토론이_실제로_영속화된다() {
            String topic = "토론 주제";

            Debate created = debateDomainRepository.create(new Debate(null, topic));
            cachedDebateIds.add(created.getId());

            DebateEntity persisted = debateJpaRepository.findById(created.getId()).orElseThrow();
            assertThat(persisted.getTopic()).isEqualTo(topic);
        }

        @Test
        void 저장과_함께_메타정보를_24시간_TTL로_캐시한다() {
            String topic = "토론 주제";
            long oneDayTtlSeconds = 86_400L;

            Debate created = debateDomainRepository.create(new Debate(null, topic));
            cachedDebateIds.add(created.getId());

            String metaKey = DebateMetaRedisCache.META_KEY_FORMAT.formatted(created.getId());
            Object cachedTopic = redisTemplate.opsForHash().get(metaKey, DebateMetaRedisCache.TOPIC_FIELD);
            Long ttl = redisTemplate.getExpire(metaKey, TimeUnit.SECONDS);

            assertAll(
                    () -> assertThat(cachedTopic).isEqualTo(topic),
                    () -> assertThat(ttl).isPositive(),
                    () -> assertThat(ttl).isLessThanOrEqualTo(oneDayTtlSeconds)
            );
        }
    }

    @Nested
    class FindById {

        @Test
        void 캐시에_메타정보가_있으면_캐시로_조합해_반환한다() {
            String dbTopic = "DB 주제";
            String cachedTopic = "캐시 주제";

            Debate created = debateGenerator.generate(dbTopic);
            cachedDebateIds.add(created.getId());
            String metaKey = DebateMetaRedisCache.META_KEY_FORMAT.formatted(created.getId());
            // 캐시 토픽만 다르게 덮어써, 조회 결과가 캐시에서 왔음을 확인한다.
            redisTemplate.opsForHash().put(metaKey, DebateMetaRedisCache.TOPIC_FIELD, cachedTopic);

            Debate found = debateDomainRepository.findById(created.getId());

            assertAll(
                    () -> assertThat(found.getId()).isEqualTo(created.getId()),
                    () -> assertThat(found.getTopic()).isEqualTo(cachedTopic)
            );
        }

        @Test
        void 캐시_미스면_DB에서_조회하고_결과를_비동기로_재적재한다() {
            String dbTopic = "DB 주제";
            long oneDayTtlSeconds = 86_400L;

            Debate created = debateGenerator.generate(dbTopic);
            cachedDebateIds.add(created.getId());
            String metaKey = DebateMetaRedisCache.META_KEY_FORMAT.formatted(created.getId());
            // generator 는 DB 만 적재하므로, 캐시 미스 전제를 명시적으로 보장한다.
            redisTemplate.delete(metaKey);

            Debate found = debateDomainRepository.findById(created.getId());

            // 반환값은 동기적으로 DB 에서 온다.
            assertThat(found.getTopic()).isEqualTo(dbTopic);
            // 재적재는 별도 스레드라 잠시 뒤 캐시에 채워진다.
            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                Object reCachedTopic = redisTemplate.opsForHash().get(metaKey, DebateMetaRedisCache.TOPIC_FIELD);
                Long ttl = redisTemplate.getExpire(metaKey, TimeUnit.SECONDS);
                assertAll(
                        () -> assertThat(reCachedTopic).isEqualTo(dbTopic),
                        () -> assertThat(ttl).isPositive(),
                        () -> assertThat(ttl).isLessThanOrEqualTo(oneDayTtlSeconds)
                );
            });
        }

        @Test
        void 존재하지_않는_토론은_예외를_던진다() {
            long missingDebateId = 999_999_999L;

            assertThatThrownBy(() -> debateDomainRepository.findById(missingDebateId))
                    .isInstanceOf(DebateTrackerException.class);
        }
    }
}
