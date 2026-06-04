package com.debatetracker.debate.service.transcript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import com.debatetracker.debate.domain.transcript.repository.SpeechBoxRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SpeechBoxServiceTest {

    private static final long DEBATE_ID = 1L;

    private SpeechBoxRepository speechBoxRepository;
    private SpeechBoxResolver speechBoxResolver;
    private SpeechBoxService service;

    @BeforeEach
    void setUp() {
        speechBoxRepository = mock(SpeechBoxRepository.class);
        speechBoxResolver = mock(SpeechBoxResolver.class);
        service = new SpeechBoxService(speechBoxRepository, speechBoxResolver);
    }

    @Nested
    class Persist {

        @Test
        void 마지막_박스_화자와_첫_박스_화자가_같으면_이어붙여_갱신하고_나머지는_저장한다() {
            List<RefinedSpeechSegment> segments = List.of(refined("a", "S1"));
            SpeechBox lastBox = new SpeechBox(7L, DEBATE_ID, "S1", "안녕", time("0.0"), time("3.0"));
            SpeechBox firstBox = new SpeechBox(null, DEBATE_ID, "S1", "오늘 주제는", time("3.0"), time("5.0"));
            SpeechBox secondBox = new SpeechBox(null, DEBATE_ID, "S2", "찬성 입론", time("5.0"), time("7.0"));
            when(speechBoxResolver.resolve(DEBATE_ID, segments)).thenReturn(List.of(firstBox, secondBox));
            when(speechBoxRepository.findLastByDebateId(DEBATE_ID)).thenReturn(Optional.of(lastBox));

            List<SpeechBox> result = service.persist(DEBATE_ID, segments);

            ArgumentCaptor<SpeechBox> updated = ArgumentCaptor.forClass(SpeechBox.class);
            verify(speechBoxRepository).update(updated.capture());
            SpeechBox merged = updated.getValue();
            assertAll(
                    () -> assertThat(merged.getId()).isEqualTo(7L),
                    () -> assertThat(merged.getContent()).isEqualTo("안녕 오늘 주제는"),
                    () -> assertThat(merged.getEndAt()).isEqualByComparingTo(time("5.0")),
                    () -> verify(speechBoxRepository).saveAll(List.of(secondBox)),
                    () -> assertThat(result).containsExactly(merged, secondBox)
            );
        }

        @Test
        void 마지막_박스_화자와_첫_박스_화자가_다르면_갱신하지_않고_전부_저장한다() {
            List<RefinedSpeechSegment> segments = List.of(refined("a", "S2"));
            SpeechBox lastBox = new SpeechBox(7L, DEBATE_ID, "S1", "안녕", time("0.0"), time("3.0"));
            SpeechBox firstBox = new SpeechBox(null, DEBATE_ID, "S2", "찬성 입론", time("3.0"), time("5.0"));
            when(speechBoxResolver.resolve(DEBATE_ID, segments)).thenReturn(List.of(firstBox));
            when(speechBoxRepository.findLastByDebateId(DEBATE_ID)).thenReturn(Optional.of(lastBox));

            List<SpeechBox> result = service.persist(DEBATE_ID, segments);

            assertAll(
                    () -> verify(speechBoxRepository, never()).update(any()),
                    () -> verify(speechBoxRepository).saveAll(List.of(firstBox)),
                    () -> assertThat(result).containsExactly(firstBox)
            );
        }

        @Test
        void 저장된_마지막_박스가_없으면_전부_저장한다() {
            List<RefinedSpeechSegment> segments = List.of(refined("a", "S1"));
            SpeechBox firstBox = new SpeechBox(null, DEBATE_ID, "S1", "첫 발화", time("0.0"), time("2.0"));
            when(speechBoxResolver.resolve(DEBATE_ID, segments)).thenReturn(List.of(firstBox));
            when(speechBoxRepository.findLastByDebateId(DEBATE_ID)).thenReturn(Optional.empty());

            service.persist(DEBATE_ID, segments);

            assertAll(
                    () -> verify(speechBoxRepository, never()).update(any()),
                    () -> verify(speechBoxRepository).saveAll(List.of(firstBox))
            );
        }

        @Test
        void 빈_세그먼트면_저장소와_리졸버를_건드리지_않고_빈_리스트를_반환한다() {
            List<SpeechBox> result = service.persist(DEBATE_ID, List.of());

            assertAll(
                    () -> assertThat(result).isEmpty(),
                    () -> verify(speechBoxResolver, never()).resolve(anyLong(), any()),
                    () -> verify(speechBoxRepository, never()).findLastByDebateId(anyLong()),
                    () -> verify(speechBoxRepository, never()).update(any()),
                    () -> verify(speechBoxRepository, never()).saveAll(any())
            );
        }
    }

    private RefinedSpeechSegment refined(String id, String speaker) {
        return new RefinedSpeechSegment(id, "교정 " + id, speaker, time("1.0"), time("2.0"));
    }

    private BigDecimal time(String value) {
        return new BigDecimal(value);
    }
}
