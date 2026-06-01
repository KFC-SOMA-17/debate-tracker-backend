package com.debatetracker.debate.document.debate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import com.debatetracker.debate.controller.debate.DebateCreateRequest;
import com.debatetracker.debate.document.BaseDocumentTest;
import com.debatetracker.debate.document.RestDocsRequest;
import com.debatetracker.debate.document.RestDocsResponse;
import com.debatetracker.debate.document.Tag;
import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.exception.ErrorCode;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.restdocs.restassured.RestDocumentationFilter;

public class DebateDocumentTest extends BaseDocumentTest {

    @Nested
    class CreateDebate {

        private final RestDocsRequest requestDocument = request()
                .tag(Tag.DEBATE_API)
                .summary("토론 생성")
                .requestBodyField(
                        fieldWithPath("topic").type(STRING).description("토론 주제")
                );

        private final RestDocsResponse responseDocument = response()
                .responseBodyField(
                        fieldWithPath("debateId").type(STRING).description("토론 ID"),
                        fieldWithPath("topic").type(STRING).description("토론 주제")
                );

        private final RestDocsRequest errorRequestDocument = request()
                .tag(Tag.DEBATE_API)
                .summary("토론 생성");

        @Test
        void 토론_생성_성공() {
            DebateCreateRequest request = new DebateCreateRequest("인공지능은 인간의 일자리를 대체할 수 있는가");
            Debate debate = new Debate(1L, "인공지능은 인간의 일자리를 대체할 수 있는가");
            doReturn(debate).when(debateService).create(any(Debate.class));

            RestDocumentationFilter document = document("debate/create", 200)
                    .request(requestDocument)
                    .response(responseDocument)
                    .build();

            given(document)
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when().post("/api/debates")
                    .then().statusCode(200);
        }

        @EnumSource(value = ErrorCode.class, names = {"FIELD_ERROR"})
        @ParameterizedTest
        void 토론_주제가_없으면_생성에_실패한다(ErrorCode errorCode) {
            DebateCreateRequest request = new DebateCreateRequest(null);

            RestDocumentationFilter document = document("debate/create", errorCode.getStatusCode())
                    .request(errorRequestDocument)
                    .response(ERROR_RESPONSE)
                    .build();

            given(document)
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when().post("/api/debates")
                    .then().statusCode(errorCode.getStatusCode());
        }
    }
}
