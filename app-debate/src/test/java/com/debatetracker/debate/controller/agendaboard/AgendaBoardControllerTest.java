package com.debatetracker.debate.controller.agendaboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.controller.BaseControllerTest;
import com.debatetracker.debate.domain.agendaboard.Agenda;
import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.EvidenceType;
import com.debatetracker.debate.domain.agendaboard.Stance;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.ClaimEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.ClaimJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.EvidenceEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.EvidenceJpaRepository;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AgendaBoardControllerTest extends BaseControllerTest {

    @Autowired
    private AgendaJpaRepository agendaJpaRepository;

    @Autowired
    private ClaimJpaRepository claimJpaRepository;

    @Autowired
    private EvidenceJpaRepository evidenceJpaRepository;

    @Nested
    class GetAgendas {

        @Test
        void 토론의_쟁점_보드를_조회한다() {
            long debateId = 500L;
            AgendaEntity agenda = agendaJpaRepository.save(
                    new AgendaEntity(new Agenda(null, debateId, "원격근무 도입이 생산성에 미치는 영향", null, null, List.of())));
            ClaimEntity claim = claimJpaRepository.save(
                    new ClaimEntity(new Claim(null, agenda.getId(), "원격근무는 생산성을 높인다", Stance.PROS, null, null, List.of())));
            evidenceJpaRepository.save(
                    new EvidenceEntity(
                            new Evidence(null, claim.getId(), "70%가 생산성 향상을 보고했다", EvidenceType.STATISTICS, null, null)));

            AgendaBoardResponse response = given()
                    .when()
                    .get("/api/debates/{debateId}/agendas", debateId)
                    .then()
                    .statusCode(200)
                    .extract().as(AgendaBoardResponse.class);

            assertAll(
                    () -> assertThat(response.debateId()).isEqualTo(debateId),
                    () -> assertThat(response.agendas()).hasSize(1),
                    () -> assertThat(response.agendas().get(0).content()).isEqualTo("원격근무 도입이 생산성에 미치는 영향"),
                    () -> assertThat(response.agendas().get(0).claims()).hasSize(1),
                    () -> assertThat(response.agendas().get(0).claims().get(0).stance()).isEqualTo(Stance.PROS),
                    () -> assertThat(response.agendas().get(0).claims().get(0).evidences()).hasSize(1),
                    () -> assertThat(response.agendas().get(0).claims().get(0).evidences().get(0).type())
                            .isEqualTo(EvidenceType.STATISTICS)
            );
        }

        @Test
        void 쟁점이_없는_토론은_빈_목록을_반환한다() {
            long debateId = 999L;

            AgendaBoardResponse response = given()
                    .when()
                    .get("/api/debates/{debateId}/agendas", debateId)
                    .then()
                    .statusCode(200)
                    .extract().as(AgendaBoardResponse.class);

            assertAll(
                    () -> assertThat(response.debateId()).isEqualTo(debateId),
                    () -> assertThat(response.agendas()).isEmpty()
            );
        }
    }
}
