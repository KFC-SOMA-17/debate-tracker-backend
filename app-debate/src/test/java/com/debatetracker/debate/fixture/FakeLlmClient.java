package com.debatetracker.debate.fixture;

import com.debatetracker.infra.llm.client.LlmClient;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class FakeLlmClient implements LlmClient {

    @Override
    public RefineResponse refine(RefineRequest request) {
        return new RefineResponse(request.targets());
    }
}
