package com.debatetracker.debate.domain.transcript;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SpeechBox {

    public static final String CONTENT_DELIMITER = " ";

    private final Long id;
    private final long debateId;
    private final String speaker;
    private final String content;
    private final BigDecimal startAt;
    private final BigDecimal endAt;

    /**
     * 같은 화자의 다음 박스를 이어붙여 새 박스를 만든다(API_DOCS §11-1 step1).
     * 식별자·시작 시각은 이 박스 것을 유지하고, content 는 뒤에 합치며, 끝 시각은 이어붙인 박스 것으로 갱신한다.
     */
    public SpeechBox append(SpeechBox next) {
        return new SpeechBox(id, debateId, speaker, content + CONTENT_DELIMITER + next.content, startAt, next.endAt);
    }
}
