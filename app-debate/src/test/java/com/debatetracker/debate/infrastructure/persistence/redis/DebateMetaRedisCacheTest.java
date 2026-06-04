package com.debatetracker.debate.infrastructure.persistence.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.config.TestcontainersConfiguration;
import com.debatetracker.debate.domain.debate.Debate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DebateMetaRedisCacheTest {

    private static final Long DEBATE_ID = 990001L;
    private static final String META_KEY = "debate:990001:meta";

    @Autowired
    private DebateMetaRedisCache cache;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    @AfterEach
    void clearMeta() {
        redisTemplate.delete(META_KEY);
    }

    @Nested
    class Save {

        @Test
        void 토론_메타정보를_해시로_저장하고_24시간_TTL을_건다() {
            cache.save(new Debate(DEBATE_ID, "토론 주제"));

            Object topic = redisTemplate.opsForHash().get(META_KEY, "topic");
            Long ttl = redisTemplate.getExpire(META_KEY, TimeUnit.SECONDS);

            assertAll(
                    () -> assertThat(topic).isEqualTo("토론 주제"),
                    () -> assertThat(ttl).isPositive(),
                    () -> assertThat(ttl).isLessThanOrEqualTo(86400L)
            );
        }
    }

    @Nested
    class Find {

        @Test
        void 캐시에_메타정보가_있으면_id와_조합해_Debate를_반환한다() {
            cache.save(new Debate(DEBATE_ID, "토론 주제"));

            Optional<Debate> found = cache.find(DEBATE_ID);

            assertAll(
                    () -> assertThat(found).isPresent(),
                    () -> assertThat(found.get().getId()).isEqualTo(DEBATE_ID),
                    () -> assertThat(found.get().getTopic()).isEqualTo("토론 주제")
            );
        }

        @Test
        void 캐시에_메타정보가_없으면_빈값을_반환한다() {
            assertThat(cache.find(DEBATE_ID)).isEmpty();
        }
    }
}
