package com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard;

import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.Stance;
import com.debatetracker.debate.infrastructure.persistence.jpa.AuditingEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "claim")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimEntity extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Claim toDomain(List<Evidence> evidences) {
        return new Claim(id, agendaId, content, stance, getCreatedAt(), getModifiedAt(), evidences);
    }
}
