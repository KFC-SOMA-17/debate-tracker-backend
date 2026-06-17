package com.debatetracker.infra.stt.adapter.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.infra.stt.config.AudioProperties;
import com.debatetracker.infra.stt.config.AzureConfig;
import com.debatetracker.infra.stt.repository.InMemoryAzureSessionRepository;
import com.debatetracker.infra.stt.service.azure.AzureSttService;
import com.debatetracker.infra.stt.session.AzureSession;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;


@ExtendWith(MockitoExtension.class)
class AzureAdapterTest {

    private static final String SESSION_ID = "test-session-1";
    private static final AzureConfig VALID_CONFIG = new AzureConfig(
            true, "test-key", "koreacentral", "ko-KR", "raw", 500
    );
    private static final AudioProperties AUDIO_PROPERTIES = new AudioProperties(16000, 1, "LINEAR16", 200);

    private InMemoryAzureSessionRepository sessionRepository;
    private AzureSttService sttService;
    private AzureAdapter adapter;

    @BeforeEach
    void setUp() {
        sessionRepository = new InMemoryAzureSessionRepository();
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        sttService = new AzureSttService(sessionRepository, eventPublisher);
        adapter = new AzureAdapter(VALID_CONFIG, AUDIO_PROPERTIES, sttService);
    }

    @Nested
    class StartStreaming {

        @Test
        void 중복_세션ID로_시작하면_무시된다() {
            injectMockSession(SESSION_ID);

            adapter.startStreaming(SESSION_ID);

            assertThat(adapter.isConnected(SESSION_ID)).isTrue();
        }

        @Test
        void 연결_실패시_예외를_던진다() {
            MockedStatic<SpeechConfig> speechConfigStatic = mockStatic(SpeechConfig.class);
            speechConfigStatic.when(() -> SpeechConfig.fromSubscription(anyString(), anyString()))
                    .thenThrow(new RuntimeException("connection error"));

            assertThatThrownBy(() -> adapter.startStreaming(SESSION_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Streaming Connection Failed");
        }
    }

    @Nested
    class StopStreaming {

        @Test
        void 스트리밍을_중지하면_세션이_제거된다() {
            AzureSession mockSession = injectMockSession(SESSION_ID);

            adapter.stopStreaming(SESSION_ID);

            assertThat(adapter.isConnected(SESSION_ID)).isFalse();
            verify(mockSession).close();
        }

        @Test
        void 존재하지_않는_세션_중지는_무시된다() {
            adapter.stopStreaming("nonexistent");

            assertThat(adapter.isConnected("nonexistent")).isFalse();
        }
    }

    @Nested
    class IsConnected {

        @Test
        void 활성_세션은_true를_반환한다() {
            injectMockSession(SESSION_ID);

            assertThat(adapter.isConnected(SESSION_ID)).isTrue();
        }

        @Test
        void 비활성_세션은_false를_반환한다() {
            assertThat(adapter.isConnected(SESSION_ID)).isFalse();
        }
    }

    @Nested
    class MultiSession {

        @Test
        void 다중_세션이_독립적으로_관리된다() {
            String sessionA = "session-a";
            String sessionB = "session-b";
            injectMockSession(sessionA);
            injectMockSession(sessionB);

            adapter.stopStreaming(sessionA);

            assertThat(adapter.isConnected(sessionA)).isFalse();
            assertThat(adapter.isConnected(sessionB)).isTrue();
        }
    }

    private AzureSession injectMockSession(String sessionId) {
        AzureSession mockSession = mock(AzureSession.class);
        when(mockSession.sessionId()).thenReturn(sessionId);
        sessionRepository.save(mockSession);
        return mockSession;
    }
}
