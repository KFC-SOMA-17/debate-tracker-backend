package com.debatetracker.debate.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.domain.agendaboard.Agenda;
import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.EvidenceType;
import com.debatetracker.debate.domain.agendaboard.Stance;
import com.debatetracker.debate.fixture.AgendaBoardGenerator;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaJpaRepository;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AgendaBoardDomainRepositoryTest extends BaseDomainRepositoryTest {

    @Autowired
    private AgendaBoardDomainRepository agendaBoardDomainRepository;

    @Autowired
    private AgendaBoardGenerator agendaBoardGenerator;

    @Autowired
    private AgendaJpaRepository agendaJpaRepository;

    @Nested
    class FindByDebateId {

        @Test
        void 토론의_쟁점_보드를_트리로_조회한다() {
            long debateId = 500L;
            Agenda agenda = agendaBoardGenerator.generateAgenda(debateId, "원격근무 도입이 생산성에 미치는 영향");
            Claim pros = agendaBoardGenerator.generateClaim(agenda.getId(), "원격근무는 생산성을 높인다", Stance.PROS);
            Claim cons = agendaBoardGenerator.generateClaim(agenda.getId(), "원격근무는 협업 효율을 떨어뜨린다", Stance.CONS);
            agendaBoardGenerator.generateEvidence(pros.getId(), "70%가 생산성 향상을 보고했다", EvidenceType.STATISTICS);
            agendaBoardGenerator.generateEvidence(pros.getId(), "이직률이 절반으로 줄었다", EvidenceType.EXAMPLE);
            agendaBoardGenerator.generateEvidence(cons.getId(), "대면 소통이 핵심이라고 말했다", EvidenceType.QUOTATION);

            AgendaBoard board = agendaBoardDomainRepository.findByDebateId(debateId);

            Agenda foundAgenda = board.getAgendas().get(0);
            Claim foundPros = foundAgenda.getClaims().stream()
                    .filter(claim -> claim.getStance() == Stance.PROS)
                    .findFirst()
                    .orElseThrow();
            Claim foundCons = foundAgenda.getClaims().stream()
                    .filter(claim -> claim.getStance() == Stance.CONS)
                    .findFirst()
                    .orElseThrow();
            assertAll(
                    () -> assertThat(board.getDebateId()).isEqualTo(debateId),
                    () -> assertThat(board.getAgendas()).hasSize(1),
                    () -> assertThat(foundAgenda.getContent()).isEqualTo("원격근무 도입이 생산성에 미치는 영향"),
                    () -> assertThat(foundAgenda.getClaims()).hasSize(2),
                    () -> assertThat(foundPros.getEvidences()).hasSize(2),
                    () -> assertThat(foundCons.getEvidences()).hasSize(1),
                    () -> assertThat(foundCons.getEvidences().get(0).getType()).isEqualTo(EvidenceType.QUOTATION),
                    () -> assertThat(foundAgenda.getCreatedAt()).isNotNull(),
                    () -> assertThat(foundAgenda.getModifiedAt()).isNotNull(),
                    () -> assertThat(foundPros.getCreatedAt()).isNotNull(),
                    () -> assertThat(foundPros.getEvidences().get(0).getCreatedAt()).isNotNull()
            );
        }

        @Test
        void 쟁점이_없는_토론은_빈_보드를_반환한다() {
            long debateId = 999_999_999L;

            AgendaBoard board = agendaBoardDomainRepository.findByDebateId(debateId);

            assertAll(
                    () -> assertThat(board.getDebateId()).isEqualTo(debateId),
                    () -> assertThat(board.getAgendas()).isEmpty()
            );
        }
    }

    @Nested
    class Upsert {

        @Test
        void 신규_보드를_upsert하면_트리가_식별자와_부모참조로_저장된다() {
            long debateId = 700L;
            AgendaBoard board = new AgendaBoard(debateId, List.of(
                    new Agenda(null, debateId, "쟁점1", null, null, List.of(
                            new Claim(null, 0L, "주장1A", Stance.PROS, null, null, List.of(
                                    new Evidence(null, 0L, "근거1", EvidenceType.STATISTICS, null, null))),
                            new Claim(null, 0L, "주장1B", Stance.CONS, null, null, List.of()))),
                    new Agenda(null, debateId, "쟁점2", null, null, List.of(
                            new Claim(null, 0L, "주장2A", Stance.PROS, null, null, List.of(
                                    new Evidence(null, 0L, "근거2", EvidenceType.EXAMPLE, null, null),
                                    new Evidence(null, 0L, "근거3", EvidenceType.QUOTATION, null, null)))))));

            agendaBoardDomainRepository.upsert(board);

            AgendaBoard found = agendaBoardDomainRepository.findByDebateId(debateId);
            Agenda agenda1 = findAgenda(found, "쟁점1");
            Agenda agenda2 = findAgenda(found, "쟁점2");
            assertAll(
                    () -> assertThat(found.getAgendas()).hasSize(2),
                    () -> assertThat(agenda1.getClaims()).hasSize(2),
                    () -> assertThat(findClaim(agenda1, Stance.PROS).getEvidences()).hasSize(1),
                    () -> assertThat(findClaim(agenda1, Stance.CONS).getEvidences()).isEmpty(),
                    () -> assertThat(agenda2.getClaims()).hasSize(1),
                    () -> assertThat(agenda2.getClaims().get(0).getEvidences()).hasSize(2)
            );
        }

        @Test
        void 기존_보드를_upsert하면_내용이_갱신되고_식별자가_보존된다() {
            long debateId = 700L;
            Agenda existing = agendaBoardGenerator.generateAgenda(debateId, "원본 쟁점");
            AgendaBoard board = new AgendaBoard(debateId, List.of(
                    new Agenda(existing.getId(), debateId, "수정된 쟁점",
                            existing.getCreatedAt(), existing.getModifiedAt(), List.of())));

            agendaBoardDomainRepository.upsert(board);

            List<AgendaEntity> agendas = agendaJpaRepository.findByDebateId(debateId);
            assertAll(
                    () -> assertThat(agendas).hasSize(1),
                    () -> assertThat(agendas.get(0).getId()).isEqualTo(existing.getId()),
                    () -> assertThat(agendas.get(0).getContent()).isEqualTo("수정된 쟁점"),
                    () -> assertThat(agendas.get(0).getModifiedAt()).isAfterOrEqualTo(existing.getModifiedAt())
            );
        }

        @Test
        void 신규와_기존이_섞인_트리를_upsert하면_모두_처리된다() {
            long debateId = 700L;
            Agenda existing = agendaBoardGenerator.generateAgenda(debateId, "기존 쟁점");
            AgendaBoard board = new AgendaBoard(debateId, List.of(
                    new Agenda(existing.getId(), debateId, "기존 쟁점 수정",
                            existing.getCreatedAt(), existing.getModifiedAt(), List.of(
                                    new Claim(null, 0L, "기존에 추가된 주장", Stance.PROS, null, null, List.of()))),
                    new Agenda(null, debateId, "신규 쟁점", null, null, List.of(
                            new Claim(null, 0L, "신규 주장", Stance.CONS, null, null, List.of())))));

            agendaBoardDomainRepository.upsert(board);

            AgendaBoard found = agendaBoardDomainRepository.findByDebateId(debateId);
            Agenda updated = findAgenda(found, "기존 쟁점 수정");
            Agenda created = findAgenda(found, "신규 쟁점");
            assertAll(
                    () -> assertThat(found.getAgendas()).hasSize(2),
                    () -> assertThat(updated.getId()).isEqualTo(existing.getId()),
                    () -> assertThat(updated.getClaims()).hasSize(1),
                    () -> assertThat(created.getId()).isNotNull(),
                    () -> assertThat(created.getClaims()).hasSize(1)
            );
        }
    }

    private Agenda findAgenda(AgendaBoard board, String content) {
        return board.getAgendas().stream()
                .filter(agenda -> agenda.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }

    private Claim findClaim(Agenda agenda, Stance stance) {
        return agenda.getClaims().stream()
                .filter(claim -> claim.getStance() == stance)
                .findFirst()
                .orElseThrow();
    }
}
