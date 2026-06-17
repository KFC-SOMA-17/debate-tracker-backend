package com.debatetracker.debate.service.debate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
import com.debatetracker.debate.service.BaseServiceTest;
import com.debatetracker.debate.service.transcript.TranscribeRefiningService;
import com.debatetracker.infra.stt.client.SttClient;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class DebateStreamingServiceTest extends BaseServiceTest {

    @Autowired
    private DebateStreamingService debateStreamingService;

    @Autowired
    private DebateSessionRepository sessionRepository;

    @MockitoBean
    private SttClient sttClient;

    @MockitoBean
    private TranscriptBufferRepository bufferRepository;

    @MockitoBean
    private TranscribeRefiningService refiningService;

    @Nested
    class StartDebate {

        @Test
        void 세션을_저장하고_전사를_시작한다() {
            String debateId = "1";

            debateStreamingService.startDebate(debateId);

            assertAll(
                    () -> verify(sttClient).startStreaming(debateId),
                    () -> assertThat(sessionRepository.existsByDebateId(debateId)).isTrue()
            );
        }
    }

    @Nested
    class StopDebateWithRemainingRefine {

        @Test
        void STT_종료_세션_삭제_후_남은_raw를_보정하고_버퍼를_정리하고_true를_반환한다() {
            String debateId = "7";
            debateStreamingService.startDebate(debateId);

            boolean stopped = debateStreamingService.stopDebateWithRemainingRefine(debateId);

            InOrder order = Mockito.inOrder(sttClient, refiningService, bufferRepository);
            assertAll(
                    () -> assertThat(stopped).isTrue(),
                    () -> order.verify(sttClient).stopStreaming(debateId),
                    () -> order.verify(refiningService).refineRemaining(any(DebateSession.class)),
                    () -> order.verify(bufferRepository).clear(debateId),
                    () -> assertThat(sessionRepository.existsByDebateId(debateId)).isFalse()
            );
        }

        @Test
        void 활성_세션이_없으면_보정도_정리도_하지_않고_false를_반환한다() {
            String debateId = "404";

            boolean stopped = debateStreamingService.stopDebateWithRemainingRefine(debateId);

            assertAll(
                    () -> assertThat(stopped).isFalse(),
                    () -> verify(sttClient, never()).stopStreaming(debateId),
                    () -> verify(refiningService, never()).refineRemaining(any(DebateSession.class))
            );
        }

        @Test
        void debateId가_null이면_아무것도_하지_않고_false를_반환한다() {
            boolean stopped = debateStreamingService.stopDebateWithRemainingRefine(null);

            assertAll(
                    () -> assertThat(stopped).isFalse(),
                    () -> verify(sttClient, never()).stopStreaming(Mockito.anyString())
            );
        }
    }

    @Nested
    class SendAudioChunk {

        @Test
        void 오디오_청크를_받으면_해당_debateId로_STT에_전달한다() {
            String debateId = "1";
            byte[] pcm = {1, 2, 3, 4};

            debateStreamingService.sendAudioChunk(debateId, pcm);

            verify(sttClient).sendAudioChunk(debateId, pcm);
        }
    }

    @Nested
    class FindActiveSessions {

        @Test
        void 시작된_세션들을_활성_세션으로_반환한다() {
            String firstDebateId = "1";
            String secondDebateId = "2";
            startDebate(firstDebateId);
            startDebate(secondDebateId);

            List<DebateSession> active = debateStreamingService.findActiveSessions();

            assertThat(active)
                    .extracting(DebateSession::debateId)
                    .containsExactlyInAnyOrder(firstDebateId, secondDebateId);
        }

        @Test
        void 시작된_세션이_없으면_빈_목록을_반환한다() {
            List<DebateSession> active = debateStreamingService.findActiveSessions();

            assertThat(active).isEmpty();
        }

        private void startDebate(String debateId) {
            debateStreamingService.startDebate(debateId);
        }
    }
}
