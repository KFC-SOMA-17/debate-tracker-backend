package com.debatetracker.debate.ws.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleSegmentIdGeneratorTest {

    private SimpleSegmentIdGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SimpleSegmentIdGenerator();
    }

    @Test
    void 생성된_id는_null이_아니고_숫자_문자열이다() {
        String id = generator.generate();

        assertThat(id).isNotNull();
        assertThat(id).containsOnlyDigits();
    }

    @Test
    void 반복_호출해도_id가_중복되지_않는다() {
        Set<String> ids = new HashSet<>();

        IntStream.range(0, 10_000).forEach(i -> ids.add(generator.generate()));

        assertThat(ids).hasSize(10_000);
    }
}
