package com.debatetracker.debate.service.transcript;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import com.debatetracker.debate.domain.transcript.repository.SpeechBoxRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RefinedSpeechSegment 들을 화자 단위 SpeechBox 로 조합해 영속화하는 유스케이스(API_DOCS §11).
 *
 * <p>리졸버로 세그먼트를 화자 단위 박스로 묶은 뒤, <b>마지막 저장 박스의 화자와 첫 박스의 화자가 같은지</b> 만으로
 * 이어붙임(update) 여부를 결정한다 — 같으면 §11-1(첫 박스를 마지막 박스에 이어붙여 갱신 + 나머지 신규 적재),
 * 다르거나 저장된 박스가 없으면 §11-2(전부 신규 적재). 저장소·리졸버하고만 소통한다.
 */
@Service
@RequiredArgsConstructor
public class SpeechBoxService {

    private final SpeechBoxRepository speechBoxRepository;
    private final SpeechBoxResolver speechBoxResolver;

    public List<SpeechBox> persist(long debateId, List<RefinedSpeechSegment> segments) {
        if (segments.isEmpty()) {
            return List.of();
        }
        List<SpeechBox> boxes = speechBoxResolver.resolve(debateId, segments);
        Optional<SpeechBox> lastBox = speechBoxRepository.findLastByDebateId(debateId);

        if (continuesLastSpeaker(lastBox, boxes.getFirst())) {
            return appendToLastBox(lastBox.get(), boxes);
        }
        speechBoxRepository.saveAll(boxes);
        return boxes;
    }

    private boolean continuesLastSpeaker(Optional<SpeechBox> lastBox, SpeechBox firstBox) {
        return lastBox.isPresent() && lastBox.get().getSpeaker().equals(firstBox.getSpeaker());
    }

    private List<SpeechBox> appendToLastBox(SpeechBox lastBox, List<SpeechBox> boxes) {
        SpeechBox merged = lastBox.append(boxes.get(0));
        speechBoxRepository.update(merged);
        List<SpeechBox> newBoxes = boxes.subList(1, boxes.size());
        speechBoxRepository.saveAll(newBoxes);

        List<SpeechBox> persisted = new ArrayList<>();
        persisted.add(merged);
        persisted.addAll(newBoxes);
        return persisted;
    }
}
