package com.debatetracker.infra.llm.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpringAiLlmClientTest {

    @Mock
    private LlmChatCaller chatCaller;

    private SpringAiLlmClient llmClient;

    @BeforeEach
    void setUp() {
        Executor sameThread = Runnable::run;
        llmClient = new SpringAiLlmClient(chatCaller, new ObjectMapper(), sameThread);
    }

    @Test
    @DisplayName("정상 응답이면 id 집합을 유지하며 text 를 반영한다")
    void refine_validResponse_appliesText() {
        List<TranscriptSegment> input = List.of(
                segment("1", "Spk1", "안녕하요"),
                segment("2", "Spk1", "반갑"));
        when(chatCaller.call(anyString(), anyString())).thenReturn("""
                [
                  {"id":"1","speaker":"Spk1","text":"안녕하세요"},
                  {"id":"2","speaker":"Spk1","text":"반갑습니다"}
                ]
                """);

        RefineResponse response = llmClient.refine(request(input)).join();

        assertThat(response.segments()).hasSize(2);
        assertThat(response.segments().get(0).text()).isEqualTo("안녕하세요");
        assertThat(response.segments().get(1).text()).isEqualTo("반갑습니다");
    }

    @Test
    @DisplayName("id/start/end 골격은 LLM 응답과 무관하게 원본을 보존하고, text·speaker 만 반영한다")
    void refine_preservesSkeleton() {
        TranscriptSegment original = segment("1", "Spk1", "원본");
        when(chatCaller.call(anyString(), anyString()))
                .thenReturn("[{\"id\":\"1\",\"speaker\":\"Spk2\",\"text\":\"정제됨\"}]");

        RefineResponse response = llmClient.refine(request(List.of(original))).join();

        TranscriptSegment refined = response.segments().getFirst();
        assertThat(refined.id()).isEqualTo("1");
        assertThat(refined.start()).isEqualByComparingTo(original.start());
        assertThat(refined.end()).isEqualByComparingTo(original.end());
        assertThat(refined.text()).isEqualTo("정제됨");
        assertThat(refined.speaker()).isEqualTo("Spk2");
    }

    @Test
    @DisplayName("코드펜스로 감싼 JSON 응답도 파싱한다")
    void refine_codeFenced_parses() {
        List<TranscriptSegment> input = List.of(segment("1", "Spk1", "원본"));
        when(chatCaller.call(anyString(), anyString())).thenReturn("""
                ```json
                [{"id":"1","speaker":"Spk1","text":"정제"}]
                ```
                """);

        RefineResponse response = llmClient.refine(request(input)).join();

        assertThat(response.segments().getFirst().text()).isEqualTo("정제");
    }

    @Test
    @DisplayName("id 가 누락된 응답이면 3회 재시도 후 원본으로 폴백한다")
    void refine_missingId_fallsBackAfterRetries() {
        List<TranscriptSegment> input = List.of(
                segment("1", "Spk1", "원본A"),
                segment("2", "Spk1", "원본B"));
        when(chatCaller.call(anyString(), anyString()))
                .thenReturn("[{\"id\":\"1\",\"speaker\":\"Spk1\",\"text\":\"하나만\"}]");

        RefineResponse response = llmClient.refine(request(input)).join();

        assertThat(response.segments()).isEqualTo(input);
        verify(chatCaller, times(3)).call(anyString(), anyString());
    }

    @Test
    @DisplayName("잉여 id 가 섞인 응답이면 폴백한다")
    void refine_extraId_fallsBack() {
        List<TranscriptSegment> input = List.of(segment("1", "Spk1", "원본"));
        when(chatCaller.call(anyString(), anyString()))
                .thenReturn("[{\"id\":\"1\",\"text\":\"a\"},{\"id\":\"99\",\"text\":\"b\"}]");

        RefineResponse response = llmClient.refine(request(input)).join();

        assertThat(response.segments()).isEqualTo(input);
        verify(chatCaller, times(3)).call(anyString(), anyString());
    }

    @Test
    @DisplayName("깨진 JSON 응답이면 폴백한다")
    void refine_brokenJson_fallsBack() {
        List<TranscriptSegment> input = List.of(segment("1", "Spk1", "원본"));
        when(chatCaller.call(anyString(), anyString())).thenReturn("이건 JSON 이 아니다");

        RefineResponse response = llmClient.refine(request(input)).join();

        assertThat(response.segments()).isEqualTo(input);
        verify(chatCaller, times(3)).call(anyString(), anyString());
    }

    @Test
    @DisplayName("빈 세그먼트 요청은 LLM 호출 없이 빈 응답을 반환한다")
    void refine_emptySegments_noCall() {
        RefineResponse response = llmClient.refine(request(List.of())).join();

        assertThat(response.segments()).isEmpty();
        verify(chatCaller, never()).call(anyString(), anyString());
    }

    private RefineRequest request(List<TranscriptSegment> segments) {
        return new RefineRequest("session-1", segments);
    }

    private TranscriptSegment segment(String id, String speaker, String text) {
        return new TranscriptSegment(id, speaker, new BigDecimal("1.0"), new BigDecimal("2.0"), text);
    }
}
