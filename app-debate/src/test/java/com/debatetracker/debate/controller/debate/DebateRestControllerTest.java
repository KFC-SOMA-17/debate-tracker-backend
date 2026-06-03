package com.debatetracker.debate.controller.debate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.controller.BaseControllerTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DebateRestControllerTest extends BaseControllerTest {

    @Nested
    class CreateDebate {

        @Test
        void 토론을_생성한다() {
            DebateCreateRequest request = new DebateCreateRequest("인공지능은 인간의 일자리를 대체할 수 있는가");

            DebateCreateResponse response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/debates")
                    .then()
                    .statusCode(200)
                    .extract().as(DebateCreateResponse.class);

            assertAll(
                    () -> assertThat(response.debateId()).isNotNull(),
                    () -> assertThat(response.topic()).isEqualTo("인공지능은 인간의 일자리를 대체할 수 있는가")
            );
        }

        @Test
        void 토론_주제가_없으면_400을_반환한다() {
            DebateCreateRequest request = new DebateCreateRequest(null);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/debates")
                    .then()
                    .statusCode(400);
        }
    }
}
