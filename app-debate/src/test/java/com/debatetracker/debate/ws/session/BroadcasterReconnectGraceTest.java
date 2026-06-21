package com.debatetracker.debate.ws.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

class BroadcasterReconnectGraceTest {

    private TaskScheduler taskScheduler;
    private BroadcasterReconnectGrace reconnectGrace;

    @BeforeEach
    void setUp() {
        taskScheduler = mock(TaskScheduler.class);
        reconnectGrace = new BroadcasterReconnectGrace(taskScheduler);
    }

    @Nested
    class ScheduleTermination {

        @Test
        void 종료_작업을_taskScheduler에_예약한다() {
            String debateId = "1";
            doReturn(mock(ScheduledFuture.class)).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

            reconnectGrace.scheduleTermination(debateId, () -> {});

            verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        void 예약된_시각이_되면_전달받은_종료_작업을_실행한다() {
            String debateId = "1";
            Runnable termination = mock(Runnable.class);
            doReturn(mock(ScheduledFuture.class)).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

            reconnectGrace.scheduleTermination(debateId, termination);

            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            verify(taskScheduler).schedule(captor.capture(), any(Instant.class));
            captor.getValue().run();
            verify(termination).run();
        }

        @Test
        void 같은_debateId로_재예약하면_이전_예약을_취소한다() {
            String debateId = "1";
            ScheduledFuture<?> first = mock(ScheduledFuture.class);
            ScheduledFuture<?> second = mock(ScheduledFuture.class);
            doReturn(first, second).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

            reconnectGrace.scheduleTermination(debateId, () -> {});
            reconnectGrace.scheduleTermination(debateId, () -> {});

            verify(first).cancel(false);
        }
    }

    @Nested
    class Cancel {

        @Test
        void 예약된_종료를_취소한다() {
            String debateId = "1";
            ScheduledFuture<?> scheduled = mock(ScheduledFuture.class);
            doReturn(scheduled).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
            reconnectGrace.scheduleTermination(debateId, () -> {});

            reconnectGrace.cancel(debateId);

            verify(scheduled).cancel(false);
        }

        @Test
        void 예약이_없으면_아무것도_하지_않는다() {
            reconnectGrace.cancel("none");

            verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        }
    }
}
