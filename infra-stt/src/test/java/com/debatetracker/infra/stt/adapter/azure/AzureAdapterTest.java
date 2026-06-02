package com.debatetracker.infra.stt.adapter.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.debatetracker.infra.stt.config.AudioProperties;
import com.debatetracker.infra.stt.config.AzureConfig;
import com.debatetracker.infra.stt.repository.AzureTranscriberSessionRepository;
import com.debatetracker.infra.stt.repository.InMemoryAzureTranscriberSessionRepository;
import com.debatetracker.infra.stt.session.AzureTranscriberSession;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class AzureAdapterTest {

    private static final String SESSION_ID = "test-session-1";
    private static final AzureConfig VALID_CONFIG = new AzureConfig(
            true, "test-key", "koreacentral", "ko-KR", "raw", 500
    );
    private static final AudioProperties AUDIO_PROPERTIES = new AudioProperties(16000, 1, "LINEAR16", 200);

    private AzureTranscriberSessionRepository sessionRepository;

    private AzureAdapter createAdapter() {
        sessionRepository = new InMemoryAzureTranscriberSessionRepository();
        return new AzureAdapter(VALID_CONFIG, AUDIO_PROPERTIES, sessionRepository);
    }

    @Nested
    class Initialization {

        @Test
        void config이_null이면_예외를_던진다() {
            assertThatThrownBy(() -> new AzureAdapter(null, AUDIO_PROPERTIES, new InMemoryAzureTranscriberSessionRepository()))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void config이_비활성이면_예외를_던진다() {
            AzureConfig disabledConfig = new AzureConfig(
                    false,
                    "test-key",
                    "koreacentral",
                    "ko-KR",
                    "raw",
                    500
            );

            assertThatThrownBy(() -> new AzureAdapter(disabledConfig, AUDIO_PROPERTIES, new InMemoryAzureTranscriberSessionRepository()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    class StartStreaming {

        @Test
        void 중복_세션ID로_시작하면_무시된다() {
            AzureAdapter adapter = createAdapter();
            injectMockSession(SESSION_ID);

            adapter.startStreaming(SESSION_ID, segment -> {
            });

            assertThat(adapter.isConnected(SESSION_ID)).isTrue();
        }

        @Test
        void 연결_실패시_예외를_던진다() {
            MockedStatic<SpeechConfig> speechConfigStatic = mockStatic(SpeechConfig.class);
            speechConfigStatic.when(() -> SpeechConfig.fromSubscription(anyString(), anyString()))
                    .thenThrow(new RuntimeException("connection error"));

            AzureAdapter adapter = createAdapter();

            assertThatThrownBy(() -> adapter.startStreaming(SESSION_ID, segment -> {
            }))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Streaming Connection Failed");
        }
    }

    @Nested
    class StopStreaming {

        @Test
        void 스트리밍을_중지하면_세션이_제거된다() {
            AzureAdapter adapter = createAdapter();
            AzureTranscriberSession mockSession = injectMockSession(SESSION_ID);

            adapter.stopStreaming(SESSION_ID);

            assertAll(
                    () -> assertThat(adapter.isConnected(SESSION_ID)).isFalse(),
                    () -> verify(mockSession).close()
            );
        }

        @Test
        void 존재하지_않는_세션_중지는_무시된다() {
            AzureAdapter adapter = createAdapter();

            assertThatCode(() -> adapter.stopStreaming("nonexistent"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class IsConnected {

        @Test
        void 활성_세션은_true를_반환한다() {
            AzureAdapter adapter = createAdapter();
            injectMockSession(SESSION_ID);

            assertThat(adapter.isConnected(SESSION_ID)).isTrue();
        }

        @Test
        void 비활성_세션은_false를_반환한다() {
            AzureAdapter adapter = createAdapter();

            assertThat(adapter.isConnected(SESSION_ID)).isFalse();
        }
    }

    @Nested
    class MultiSession {

        @Test
        void 다중_세션이_독립적으로_관리된다() {
            AzureAdapter adapter = createAdapter();
            String sessionA = "session-a";
            String sessionB = "session-b";
            injectMockSession(sessionA);
            injectMockSession(sessionB);

            adapter.stopStreaming(sessionA);

            assertAll(
                    () -> assertThat(adapter.isConnected(sessionA)).isFalse(),
                    () -> assertThat(adapter.isConnected(sessionB)).isTrue()
            );
        }
    }

    // --- 헬퍼 메서드 ---
    private AzureTranscriberSession injectMockSession(String sessionId) {
        AzureTranscriberSession mockSession = mock(AzureTranscriberSession.class);
        org.mockito.Mockito.when(mockSession.sessionId()).thenReturn(sessionId);
        sessionRepository.save(mockSession);
        return mockSession;
    }
}
