package com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard;

import com.debatetracker.debate.domain.agendaboard.Agenda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "agenda")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "debate_id")
    private long debateId;

    private String content;

    public AgendaEntity(Agenda domain) {
        this.id = domain.getId();
        this.debateId = domain.getDebateId();
        this.content = domain.getContent();
    }

    public Agenda toDomain() {
        return new Agenda(id, debateId, content);
    }
}
