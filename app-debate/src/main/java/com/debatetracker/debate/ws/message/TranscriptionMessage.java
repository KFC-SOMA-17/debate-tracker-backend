package com.debatetracker.debate.ws.message;

import com.debatetracker.debate.domain.transcript.SpeechSegment;

public class TranscriptionMessage extends WebSocketMessage {

    private final SpeechSegment segment;

    public TranscriptionMessage(long debateId, SpeechSegment segment) {
        super(debateId, MessageType.TRANSCRIPTION);
        this.segment = segment;
    }

    @Override
    public SpeechSegment data() {
        return segment;
    }
}
