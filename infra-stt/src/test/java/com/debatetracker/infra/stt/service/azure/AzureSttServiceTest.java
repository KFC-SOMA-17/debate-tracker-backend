package com.debatetracker.infra.stt.service.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.infra.stt.client.event.TranscribeEvent;
import com.debatetracker.infra.stt.repository.AzureSessionRepository;
import com.debatetracker.infra.stt.repository.InMemoryAzureSessionRepository;
import com.debatetracker.infra.stt.session.AzureSession;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionEventArgs;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionResult;
import java.math.BigInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AzureSttServiceTest {

    private static final String SESSION_ID = "test-session-1";

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AzureSessionRepository sessionRepository;
    private AzureSttService sttService;

    @BeforeEach
    void setUp() {
        sessionRepository = new InMemoryAzureSessionRepository();
        sttService = new AzureSttService(sessionRepository, eventPublisher);
    }

    @Nested
    class HandleStartStreaming {

        @Test
        void 세션을_저장한다() {
            AzureSession session = mock(AzureSession.class);
            when(session.sessionId()).thenReturn(SESSION_ID);

            sttService.handleStartStreaming(SESSION_ID, session);

            assertThat(sessionRepository.existsBySessionId(SESSION_ID)).isTrue();
        }
    }

    @Nested
    class HandleSendAudioChunk {

        @Test
        void 활성_세션에_오디오를_전송한다() {
            PushAudioInputStream pushStream = mock(PushAudioInputStream.class);
            AzureSession session = mock(AzureSession.class);
            when(session.sessionId()).thenReturn(SESSION_ID);
            when(session.pushStream()).thenReturn(pushStream);
            sessionRepository.save(session);

            byte[] pcmData = new byte[]{1, 2, 3};
            sttService.handleSendAudioChunk(SESSION_ID, pcmData);

            verify(pushStream).write(pcmData);
        }

        @Test
        void 비활성_세션이면_무시한다() {
            byte[] pcmData = new byte[]{1, 2, 3};

            sttService.handleSendAudioChunk("nonexistent", pcmData);

            assertThat(sessionRepository.existsBySessionId("nonexistent")).isFalse();
        }
    }

    @Nested
    class HandleStopStreaming {

        @Test
        void 세션을_삭제하고_close를_호출한다() {
            AzureSession session = mock(AzureSession.class);
            when(session.sessionId()).thenReturn(SESSION_ID);
            sessionRepository.save(session);

            sttService.handleStopStreaming(SESSION_ID);

            assertAll(
                    () -> assertThat(sessionRepository.existsBySessionId(SESSION_ID)).isFalse(),
                    () -> verify(session).close()
            );
        }

        @Test
        void 존재하지_않는_세션이면_무시한다() {
            sttService.handleStopStreaming("nonexistent");

            assertThat(sessionRepository.existsBySessionId("nonexistent")).isFalse();
        }
    }

    @Nested
    class IsSessionActive {

        @Test
        void 활성_세션은_true를_반환한다() {
            AzureSession session = mock(AzureSession.class);
            when(session.sessionId()).thenReturn(SESSION_ID);
            sessionRepository.save(session);

            assertThat(sttService.isSessionActive(SESSION_ID)).isTrue();
        }

        @Test
        void 비활성_세션은_false를_반환한다() {
            assertThat(sttService.isSessionActive(SESSION_ID)).isFalse();
        }
    }

    @Nested
    class HandleTranscribed {

        @Test
        void 인식_결과를_TranscribeEvent로_발행한다() {
            ConversationTranscriptionEventArgs eventArgs = mock(ConversationTranscriptionEventArgs.class);
            ConversationTranscriptionResult result = mock(ConversationTranscriptionResult.class);
            when(eventArgs.getResult()).thenReturn(result);
            when(result.getReason()).thenReturn(ResultReason.RecognizedSpeech);
            when(result.getText()).thenReturn("안녕하세요");
            when(result.getSpeakerId()).thenReturn("Speaker1");
            when(result.getOffset()).thenReturn(BigInteger.valueOf(10_000_000L));
            when(result.getDuration()).thenReturn(BigInteger.valueOf(5_000_000L));

            sttService.handleTranscribed(SESSION_ID, eventArgs);

            ArgumentCaptor<TranscribeEvent> captor = ArgumentCaptor.forClass(TranscribeEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            TranscribeEvent event = captor.getValue();
            assertAll(
                    () -> assertThat(event.sessionId()).isEqualTo(SESSION_ID),
                    () -> assertThat(event.segment().content()).isEqualTo("안녕하세요"),
                    () -> assertThat(event.segment().speaker()).isEqualTo("Speaker1")
            );
        }

        @Test
        void 텍스트가_비어있으면_이벤트를_발행하지_않는다() {
            ConversationTranscriptionEventArgs eventArgs = mock(ConversationTranscriptionEventArgs.class);
            ConversationTranscriptionResult result = mock(ConversationTranscriptionResult.class);
            when(eventArgs.getResult()).thenReturn(result);
            when(result.getReason()).thenReturn(ResultReason.RecognizedSpeech);
            when(result.getText()).thenReturn("");

            sttService.handleTranscribed(SESSION_ID, eventArgs);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void ResultReason이_RecognizedSpeech가_아니면_이벤트를_발행하지_않는다() {
            ConversationTranscriptionEventArgs eventArgs = mock(ConversationTranscriptionEventArgs.class);
            ConversationTranscriptionResult result = mock(ConversationTranscriptionResult.class);
            when(eventArgs.getResult()).thenReturn(result);
            when(result.getReason()).thenReturn(ResultReason.NoMatch);

            sttService.handleTranscribed(SESSION_ID, eventArgs);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class HandleSessionStopped {

        @Test
        void 세션을_삭제한다() {
            AzureSession session = mock(AzureSession.class);
            when(session.sessionId()).thenReturn(SESSION_ID);
            sessionRepository.save(session);

            sttService.handleSessionStopped(SESSION_ID);

            assertThat(sessionRepository.existsBySessionId(SESSION_ID)).isFalse();
        }
    }
}
