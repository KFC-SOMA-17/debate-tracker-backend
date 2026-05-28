package com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard;

import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Stance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "claim")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agenda_id")
    private long agendaId;

    @NotBlank
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Stance stance;

    public ClaimEntity(Claim domain) {
        this.id = domain.getId();
        this.agendaId = domain.getAgendaId();
        this.content = domain.getContent();
        this.stance = domain.getStance();
    }

    public Claim toDomain() {
        return new Claim(id, agendaId, content, stance);
    }
}
