package com.debatetracker.debate.document.agendaboard;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;

import com.debatetracker.debate.document.BaseDocumentTest;
import com.debatetracker.debate.document.RestDocsRequest;
import com.debatetracker.debate.document.RestDocsResponse;
import com.debatetracker.debate.document.Tag;
import com.debatetracker.debate.domain.agendaboard.Agenda;
import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.EvidenceType;
import com.debatetracker.debate.domain.agendaboard.Stance;
import com.debatetracker.debate.service.agendaboard.AgendaBoardService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.restassured.RestDocumentationFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class AgendaBoardDocumentTest extends BaseDocumentTest {

    @MockitoBean
    protected AgendaBoardService agendaBoardService;

    @Nested
    class GetAgendas {

        private final RestDocsRequest requestDocument = request()
                .tag(Tag.AGENDA_API)
                .summary("쟁점 보드 조회")
                .pathParameter(
                        parameterWithName("debateId").description("토론 ID")
                );

        private final RestDocsResponse responseDocument = response()
                .responseBodyField(
                        fieldWithPath("debateId").type(NUMBER).description("토론 ID"),
                        fieldWithPath("agendas[]").type(ARRAY).description("쟁점 리스트"),
                        fieldWithPath("agendas[].agendaId").type(NUMBER).description("쟁점 ID"),
                        fieldWithPath("agendas[].content").type(STRING).description("쟁점 내용"),
                        fieldWithPath("agendas[].createdAt").type(STRING).description("쟁점 생성 일시"),
                        fieldWithPath("agendas[].modifiedAt").type(STRING).description("쟁점 수정 일시"),
                        fieldWithPath("agendas[].claims[]").type(ARRAY).description("주장 리스트"),
                        fieldWithPath("agendas[].claims[].claimId").type(NUMBER).description("주장 ID"),
                        fieldWithPath("agendas[].claims[].content").type(STRING).description("주장 내용"),
                        fieldWithPath("agendas[].claims[].stance").type(STRING).description("주장 입장 (PROS, CONS)"),
                        fieldWithPath("agendas[].claims[].createdAt").type(STRING).description("주장 생성 일시"),
                        fieldWithPath("agendas[].claims[].modifiedAt").type(STRING).description("주장 수정 일시"),
                        fieldWithPath("agendas[].claims[].evidences[]").type(ARRAY).description("근거 리스트"),
                        fieldWithPath("agendas[].claims[].evidences[].evidenceId").type(NUMBER).description("근거 ID"),
                        fieldWithPath("agendas[].claims[].evidences[].content").type(STRING).description("근거 내용"),
                        fieldWithPath("agendas[].claims[].evidences[].type").type(STRING)
                                .description("근거 유형 (STATISTICS, EXAMPLE, QUOTATION)"),
                        fieldWithPath("agendas[].claims[].evidences[].createdAt").type(STRING).description("근거 생성 일시"),
                        fieldWithPath("agendas[].claims[].evidences[].modifiedAt").type(STRING).description("근거 수정 일시")
                );

        @Test
        void 쟁점_보드_조회_성공() {
            LocalDateTime now = LocalDateTime.of(2026, 6, 4, 22, 15, 30);
            AgendaBoard board = new AgendaBoard(500L, List.of(
                    new Agenda(1001L, 500L, "원격근무 도입이 생산성에 미치는 영향", now, now, List.of(
                            new Claim(2001L, 1001L, "원격근무는 생산성을 높인다", Stance.PROS, now, now, List.of(
                                    new Evidence(3001L, 2001L, "재택근무 도입 기업의 70%가 생산성 향상을 보고했다",
                                            EvidenceType.STATISTICS, now, now)))))));
            doReturn(board).when(agendaBoardService).findByDebateId(anyLong());

            RestDocumentationFilter document = document("agenda/findAll", 200)
                    .request(requestDocument)
                    .response(responseDocument)
                    .build();

            given(document)
                    .when().get("/api/debates/{debateId}/agendas", 500L)
                    .then().statusCode(200);
        }
    }
}
