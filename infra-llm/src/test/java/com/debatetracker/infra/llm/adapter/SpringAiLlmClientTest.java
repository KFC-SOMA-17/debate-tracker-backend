package com.debatetracker.infra.llm.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.debatetracker.infra.llm.chat.refine.RefineLlmChat;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpringAiLlmClientTest {

    @Mock
    private RefineLlmChat refineLlmChat;

    @Test
    void refine_delegatesToRefineLlmChat() {
        SpringAiLlmClient client = new SpringAiLlmClient(refineLlmChat);
        RefineRequest request = new RefineRequest("s1", "주제", List.of(), List.of(segment("1")));
        RefineResponse expected = new RefineResponse(List.of(segment("1")));
        given(refineLlmChat.fetch(request)).willReturn(expected);

        RefineResponse result = client.refine(request);

        assertThat(result).isSameAs(expected);
        verify(refineLlmChat).fetch(request);
    }

    private static TranscriptSegment segment(String id) {
        return new TranscriptSegment(id, "A", new BigDecimal("0.0"), new BigDecimal("1.0"), "발화");
    }
}
