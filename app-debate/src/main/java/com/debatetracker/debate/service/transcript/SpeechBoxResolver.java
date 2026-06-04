package com.debatetracker.debate.service.transcript;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

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
