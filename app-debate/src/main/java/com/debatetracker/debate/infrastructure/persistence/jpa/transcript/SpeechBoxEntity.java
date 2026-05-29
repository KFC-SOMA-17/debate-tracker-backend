package com.debatetracker.debate.infrastructure.persistence.jpa.transcript;

import com.debatetracker.debate.domain.transcript.SpeechBox;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "speech_segment")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpeechBoxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "debate_id")
    private long debateId;

    @NotBlank
    private String speaker;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(precision = 10, scale = 3)
    private BigDecimal startAt;

    @Column(precision = 10, scale = 3)
    private BigDecimal endAt;

    public SpeechBoxEntity(SpeechBox domain) {
        this.id = domain.getId();
        this.debateId = domain.getDebateId();
        this.speaker = domain.getSpeaker();
        this.startAt = domain.getStartAt();
        this.endAt = domain.getEndAt();
        this.content = domain.getContent();
    }

    public SpeechBox toDomain() {
        return new SpeechBox(id, debateId, speaker, content, startAt, endAt);
    }
}
