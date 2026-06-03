package com.debatetracker.debate.ws.id;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 단순 단조 증가 기반 세그먼트 ID 생성기. 추후 분산 Snowflake 구현으로 교체 가능하도록 인터페이스 뒤에 둔다.
 */
@Component
public class SimpleSegmentIdGenerator implements SegmentIdGenerator {

    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() << 20);

    @Override
    public String generate() {
        return Long.toString(sequence.incrementAndGet());
    }
}
