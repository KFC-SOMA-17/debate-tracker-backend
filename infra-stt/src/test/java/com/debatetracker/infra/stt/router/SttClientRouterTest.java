package com.debatetracker.infra.stt.router;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.infra.stt.logger.SttLogger;
import com.debatetracker.infra.stt.repository.InMemorySttSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SttClientRouterTest {

    private static final String SESSION_ID = "test-session-1";

    private SttSessionCreator sessionCreator;
    private InMemorySttSessionRepository sessionRepository;
    private SttLogger sttLogger;
    private SttClientRouter router;

    @BeforeEach
    void setUp() {
        sessionCreator = mock(SttSessionCreator.class);
        sessionRepository = new InMemorySttSessionRepository();
        sttLogger = mock(SttLogger.class);
        router = new SttClientRouter(sessionCreator, sessionRepository, sttLogger);
    }

    @Nested
    class StartStreaming {

        @Test
        void 세션을_생성하고_저장한다() {
            SttSession session = mock(SttSession.class);
            when(sessionCreator.create(SESSION_ID)).thenReturn(session);

            router.startStreaming(SESSION_ID);

            assertThat(sessionRepository.existsBySessionId(SESSION_ID)).isTrue();
        }

        @Test
        void 중복_세션ID로_시작하면_무시된다() {
            SttSession existing = mock(SttSession.class);
            sessionRepository.save(SESSION_ID, existing);

            router.startStreaming(SESSION_ID);

            verify(sessionCreator, never()).create(SESSION_ID);
        }
    }

    @Nested
    class SendAudioChunk {

        @Test
        void 활성_세션에_오디오를_전송한다() {
            SttSession session = mock(SttSession.class);
            sessionRepository.save(SESSION_ID, session);
            byte[] pcmData = new byte[]{1, 2, 3};

            router.sendAudioChunk(SESSION_ID, pcmData);

            verify(session).sendAudio(pcmData);
        }

        @Test
        void 비활성_세션이면_무시한다() {
            byte[] pcmData = new byte[]{1, 2, 3};

            router.sendAudioChunk("nonexistent", pcmData);

            assertThat(sessionRepository.existsBySessionId("nonexistent")).isFalse();
        }
    }

    @Nested
    class StopStreaming {

        @Test
        void 스트리밍을_중지하면_세션이_제거되고_stop이_호출된다() {
            SttSession session = mock(SttSession.class);
            sessionRepository.save(SESSION_ID, session);

            router.stopStreaming(SESSION_ID);

            assertAll(
                    () -> assertThat(sessionRepository.existsBySessionId(SESSION_ID)).isFalse(),
                    () -> verify(session).stop()
            );
        }

        @Test
        void 존재하지_않는_세션_중지는_무시된다() {
            router.stopStreaming("nonexistent");

            assertThat(sessionRepository.existsBySessionId("nonexistent")).isFalse();
        }
    }

    @Nested
    class IsConnected {

        @Test
        void 세션이_존재하고_연결되어_있으면_true를_반환한다() {
            SttSession session = mock(SttSession.class);
            when(session.isConnected()).thenReturn(true);
            sessionRepository.save(SESSION_ID, session);

            assertThat(router.isConnected(SESSION_ID)).isTrue();
        }

        @Test
        void 세션이_없으면_false를_반환한다() {
            assertThat(router.isConnected(SESSION_ID)).isFalse();
        }
    }

    @Nested
    class MultiSession {

        @Test
        void 다중_세션이_독립적으로_관리된다() {
            String sessionA = "session-a";
            String sessionB = "session-b";
            SttSession mockA = mock(SttSession.class);
            SttSession mockB = mock(SttSession.class);
            sessionRepository.save(sessionA, mockA);
            sessionRepository.save(sessionB, mockB);

            router.stopStreaming(sessionA);

            assertAll(
                    () -> assertThat(sessionRepository.existsBySessionId(sessionA)).isFalse(),
                    () -> assertThat(sessionRepository.existsBySessionId(sessionB)).isTrue()
            );
        }
    }
}
