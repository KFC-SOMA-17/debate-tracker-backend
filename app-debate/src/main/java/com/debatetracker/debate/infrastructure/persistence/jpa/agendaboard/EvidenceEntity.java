package com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard;

import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.EvidenceType;

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
@Table(name = "evidence")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvidenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id")
    private long claimId;

    @NotBlank
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EvidenceType type;

    public EvidenceEntity(Evidence domain) {
        this.id = domain.getId();
        this.claimId = domain.getClaimId();
        this.content = domain.getContent();
        this.type = domain.getType();
    }

    public Evidence toDomain() {
        return new Evidence(id, claimId, content, type);
    }
}
