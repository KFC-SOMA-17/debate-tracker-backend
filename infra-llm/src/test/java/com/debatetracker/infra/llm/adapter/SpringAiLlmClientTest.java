package com.debatetracker.infra.llm.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.debatetracker.infra.llm.chat.extract.ExtractLlmChat;
import com.debatetracker.infra.llm.chat.refine.RefineLlmChat;
import com.debatetracker.infra.llm.client.ExtractAgenda;
import com.debatetracker.infra.llm.client.ExtractAgendaRequest;
import com.debatetracker.infra.llm.client.ExtractAgendaResponse;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpringAiLlmClientTest {

    @Mock
    private RefineLlmChat refineLlmChat;

    @Mock
    private ExtractLlmChat extractLlmChat;

    @Nested
    class Refine {

        @Test
        void 요청을_RefineLlmChat에_위임한다() {
            SpringAiLlmClient client = new SpringAiLlmClient(refineLlmChat, extractLlmChat);
            RefineRequest request = new RefineRequest("s1", "주제", List.of(), List.of(segment("1")));
            RefineResponse expected = new RefineResponse(List.of(segment("1")));
            given(refineLlmChat.fetch(request)).willReturn(expected);

            RefineResponse result = client.refine(request);

            assertAll(
                    () -> assertThat(result).isSameAs(expected),
                    () -> verify(refineLlmChat).fetch(request)
            );
        }
    }

    @Nested
    class Extract {

        @Test
        void 요청을_ExtractLlmChat에_위임한다() {
            SpringAiLlmClient client = new SpringAiLlmClient(refineLlmChat, extractLlmChat);
            ExtractAgendaRequest request = new ExtractAgendaRequest("s1", List.of(segment("1")), List.of());
            ExtractAgendaResponse expected = new ExtractAgendaResponse(List.of(new ExtractAgenda(null, "쟁점", List.of())));
            given(extractLlmChat.fetch(request)).willReturn(expected);

            ExtractAgendaResponse result = client.extract(request);

            assertAll(
                    () -> assertThat(result).isSameAs(expected),
                    () -> verify(extractLlmChat).fetch(request)
            );
        }
    }

    private static TranscriptSegment segment(String id) {
        return new TranscriptSegment(id, "A", new BigDecimal("0.0"), new BigDecimal("1.0"), "발화");
    }
}
