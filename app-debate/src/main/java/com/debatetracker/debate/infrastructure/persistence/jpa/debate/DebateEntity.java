package com.debatetracker.debate.infrastructure.persistence.jpa.debate;

import com.debatetracker.debate.domain.debate.Debate;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "debate")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DebateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String topic;

    public DebateEntity(Debate domain) {
        this.id = domain.getId();
        this.topic = domain.getTopic();
    }

    public Debate toDomain() {
        return new Debate(id, topic);
    }
}
