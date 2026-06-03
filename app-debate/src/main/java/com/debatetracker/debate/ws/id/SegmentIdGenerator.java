package com.debatetracker.debate.ws.id;

public interface SegmentIdGenerator {

    /**
     * 전사 세그먼트의 고유 식별자를 발급한다. JSON 정밀도 손실 방지를 위해 문자열로 반환한다.
     */
    String generate();
}
