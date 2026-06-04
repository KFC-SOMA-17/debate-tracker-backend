package com.debatetracker.debate.service.transcript;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RefinedSpeechSegment(Azure STT 한 발화 단위) 들을 연속된 같은 화자끼리 묶어 SpeechBox(한 화자의 연속 발언)
 * 로 변환하는 순수 로직(API_DOCS §11 step2). I/O 가 없고, 마지막 저장 박스와의 이어붙임 판단은 하지 않는다 —
 * 그 판단(화자 비교)과 영속화는 {@link SpeechBoxService} 의 책임이다. 만들어진 박스는 모두 신규(id={@code null})다.
 */
@Component
public class SpeechBoxResolver {

    public List<SpeechBox> resolve(long debateId, List<RefinedSpeechSegment> segments) {
        if (segments.isEmpty()) {
            return List.of();
        }
        List<SpeechBox> boxes = new ArrayList<>();
        List<RefinedSpeechSegment> group = new ArrayList<>();
        for (RefinedSpeechSegment segment : segments) {
            if (!group.isEmpty() && !group.get(0).getSpeaker().equals(segment.getSpeaker())) {
                boxes.add(toBox(debateId, group));
                group = new ArrayList<>();
            }
            group.add(segment);
        }
        boxes.add(toBox(debateId, group));
        return boxes;
    }

    private SpeechBox toBox(long debateId, List<RefinedSpeechSegment> group) {
        RefinedSpeechSegment first = group.get(0);
        RefinedSpeechSegment last = group.get(group.size() - 1);
        return new SpeechBox(null, debateId, first.getSpeaker(), joinContents(group), first.getStartAt(), last.getEndAt());
    }

    private String joinContents(List<RefinedSpeechSegment> group) {
        return group.stream()
                .map(RefinedSpeechSegment::getContent)
                .reduce((left, right) -> left + SpeechBox.CONTENT_DELIMITER + right)
                .orElse("");
    }
}
