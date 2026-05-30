package com.debatetracker.debate.document;

import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.restassured.RestAssuredRestDocumentation;
import org.springframework.restdocs.restassured.RestAssuredRestDocumentationConfigurer;
import org.springframework.restdocs.restassured.RestDocumentationFilter;

@ExtendWith({RestDocumentationExtension.class, MockitoExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseDocumentTest {

    protected static final RestDocsResponse ERROR_RESPONSE = new RestDocsResponse()
            .responseBodyField(
                    fieldWithPath("errorCode").type(STRING).description("에러 코드"),
                    fieldWithPath("message").type(STRING).description("에러 메시지")
            );

    @LocalServerPort
    private int port;

    private RequestSpecification spec;

    @BeforeEach
    void setEnvironment(RestDocumentationContextProvider restDocumentation) {
        RestAssured.port = port;
        RestAssuredRestDocumentationConfigurer webConfigurer =
                RestAssuredRestDocumentation.documentationConfiguration(restDocumentation);
        spec = new RequestSpecBuilder()
                .addFilter(webConfigurer)
                .build();
    }

    protected final RestDocsRequest request() {
        return new RestDocsRequest();
    }

    protected final RestDocsResponse response() {
        return new RestDocsResponse();
    }

    protected final RestDocsFilterBuilder document(String identifierPrefix, int statusCode) {
        return new RestDocsFilterBuilder(identifierPrefix, Integer.toString(statusCode));
    }

    protected final RequestSpecification given(RestDocumentationFilter documentationFilter) {
        return RestAssured.given(spec)
                .filter(documentationFilter);
    }
}
