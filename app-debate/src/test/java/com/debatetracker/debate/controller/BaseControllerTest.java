package com.debatetracker.debate.controller;

import com.debatetracker.debate.DatabaseCleaner;
import com.debatetracker.debate.config.TestcontainersConfiguration;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@ExtendWith(DatabaseCleaner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseControllerTest {

    private static final List<Filter> SPEC_FILTERS = List.of(
            new RequestLoggingFilter(),
            new ResponseLoggingFilter()
    );

    @LocalServerPort
    private int port;

    private RequestSpecification spec;

    @BeforeEach
    final void setEnvironment() {
        RestAssured.port = port;
        spec = new RequestSpecBuilder()
                .addFilters(SPEC_FILTERS)
                .build();
    }


    protected final RequestSpecification given() {
        return RestAssured.given(spec);
    }
}
