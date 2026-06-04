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
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscribeRefiningService {

    private static final int MAX_BATCH = 5; //세그먼트 개수로 트리거
    private static final int CONTEXT_SIZE = 5;

    private final TranscriptBufferRepository bufferRepository;
    private final UtteranceCorrector corrector;
    private final WebSocketMessageSender messageSender;
    private final DebateRepository debateRepository;
    private final SpeechBoxService speechBoxService; //TODO 추상화 의존성 무너짐 -> Facade 고려

    public void refineRemaining(DebateSession session) {
        String debateId = session.debateId();
        int count = (int) bufferRepository.rawSize(debateId);
        refine(session, count);
    }

    public void refineSession(DebateSession session) {
        String debateId = session.debateId();
        int count = (int) Math.min(bufferRepository.rawSize(debateId), MAX_BATCH);
        refine(session, count);
    }

    private void refine(DebateSession session, int count) {
        String debateId = session.debateId();
        if (count == 0) {
            return;
        }
        // 보정 이후 후속 3작업(buffer 갱신·SpeechBox 영속화·WebSocket 전송)을 한 사이클당 eventId 로 묶어 추적한다.
        String eventId = UUID.randomUUID().toString();
        try {
            String topic = debateRepository.findById(Long.parseLong(debateId)).getTopic();
            List<SpeechSegment> batch = bufferRepository.peekRaw(debateId, count);
            List<RefinedSpeechSegment> context = bufferRepository.recentRefined(debateId, CONTEXT_SIZE);

            List<RefinedSpeechSegment> corrected = corrector.refine(debateId, resolveTopic(debateId), context, batch);
            validate(corrected, batch);

            long debateIdValue = Long.parseLong(debateId);

            bufferRepository.trimRaw(debateId, count);
            bufferRepository.appendRefined(debateId, corrected);
            log.info("[refineEvent={}] buffer 갱신 완료: debateId={}, raw -{}건, refined +{}건",
                    eventId, debateId, count, corrected.size());

            // TODO: SpeechBox 영속화(DB)와 WebSocket broadcast 는 서로 독립적이므로 병렬화 고려.
            //  - persist 는 DB I/O 라 broadcast 를 블로킹하지 않도록 @Async 로 분리(또는 둘을 CompletableFuture 로 병렬 실행).
            //  - 단 persist 와 appendRefined 의 순서/정합성(같은 corrected 기준)과 실패 시 재시도 정책을 함께 설계해야 함.
            //  - 병렬화 시에도 같은 eventId 를 각 작업 로그에 넘겨 사이클 단위 추적을 유지할 것.
            speechBoxService.persist(debateIdValue, corrected);
            log.info("[refineEvent={}] SpeechBox 영속화 완료: debateId={}, {}건", eventId, debateId, corrected.size());

            messageSender.send(session, new RefinedTranscriptionMessage(debateIdValue, corrected));
            log.info("[refineEvent={}] WebSocket 전송 완료: debateId={}, {}건", eventId, debateId, corrected.size());

            log.debug("[refineEvent={}] 전사 보정 사이클 완료: debateId={}, {}건", eventId, debateId, corrected.size());
        } catch (Exception e) {
            log.error("[refineEvent={}] 전사 보정 실패 — raw 를 유지하고 다음 틱에 재시도: debateId={}", eventId, debateId, e);
        }
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
}
