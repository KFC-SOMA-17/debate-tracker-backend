package com.debatetracker.debate.service.transcript;

import com.debatetracker.debate.domain.debate.DebateRepository;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
import com.debatetracker.debate.ws.message.RefinedTranscriptionMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * 한 토론 세션의 raw 전사 버퍼를 LLM 으로 보정한다. 트리거 주기(스케줄링)는
 * {@code scheduler.TranscribeRefiningScheduler} 가 담당하고, 본 서비스는 세션 1개의 보정 로직만 가진다.
 *
 * <p>raw 앞쪽 최대 {@link #MAX_BATCH} 개와 최근 refined {@link #CONTEXT_SIZE} 개를 함께 보정 요청하고,
 * 성공하면 처리한 raw 를 제거하고 교정본을 refined 에 축적해 REFINED_TRANSCRIPTION 으로 전송한다.
 *
 * <p>보정 호출이 실패하면 {@link Retryable} 이 {@link #MAX_ATTEMPTS} 회까지 즉시 재시도하고, 모두 소진되면
 * {@link #recover}} 가 raw 를 그대로 둔 채 종료해 다음 스케줄 틱에서 다시 시도되도록 한다. (AOP 프록시 기반이라
 * 반드시 다른 빈(스케줄러)에서 호출되어야 재시도가 적용된다 — self-invocation 금지.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscribeRefiningService {

    private static final int MAX_BATCH = 5; //세그먼트 개수로 트리거
    private static final int CONTEXT_SIZE = 5;
    private static final int MAX_ATTEMPTS = 3; //재시도 횟수
    private static final long BACKOFF_DELAY_MS = 300L;

    private final TranscriptBufferRepository bufferRepository;
    private final UtteranceCorrector corrector;
    private final WebSocketMessageSender messageSender;
    private final DebateRepository debateRepository;

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = MAX_ATTEMPTS,
            backoff = @Backoff(delay = BACKOFF_DELAY_MS)
    )
    public void refineSession(DebateSession session) {
        String debateId = session.debateId();
        int count = (int) Math.min(bufferRepository.rawSize(debateId), MAX_BATCH);
        if (count == 0) {
            return;
        }
        List<SpeechSegment> batch = bufferRepository.peekRaw(debateId, count);
        List<RefinedSpeechSegment> context = bufferRepository.recentRefined(debateId, CONTEXT_SIZE);

        List<RefinedSpeechSegment> corrected = corrector.refine(resolveTopic(debateId), context, batch);
        validate(corrected, batch);

        bufferRepository.trimRaw(debateId, count);
        bufferRepository.appendRefined(debateId, corrected);
        messageSender.send(session, new RefinedTranscriptionMessage(Long.parseLong(debateId), corrected));
        log.debug("전사 보정 완료: debateId={}, {}건", debateId, corrected.size());
    }

    @Recover
    public void recover(Exception cause, DebateSession session) {
        log.error("전사 보정 재시도 소진 — raw 를 유지하고 다음 틱에 재시도: debateId={}", session.debateId(), cause);
    }

    private void validate(List<RefinedSpeechSegment> corrected, List<SpeechSegment> batch) {
        if (corrected == null || corrected.size() != batch.size()) {
            log.error("보정 결과 개수 불일치: 요청={}건, 응답={}", batch.size(), corrected == null ? null : corrected.size());
            throw new DebateTrackerException(ErrorCode.REFINE_RESULT_MISMATCH);
        }
        Set<String> batchIds = batch.stream().map(SpeechSegment::getId).collect(Collectors.toSet());
        Set<String> correctedIds = corrected.stream().map(RefinedSpeechSegment::getId).collect(Collectors.toSet());
        if (!batchIds.equals(correctedIds)) {
            log.error("보정 결과 id 집합 불일치: 요청={}, 응답={}", batchIds, correctedIds);
            throw new DebateTrackerException(ErrorCode.REFINE_RESULT_MISMATCH);
        }
    }

    private String resolveTopic(String debateId) {
        return debateRepository.findById(Long.parseLong(debateId)).getTopic();
    }
}
