package com.debatetracker.debate.fixture;

import com.debatetracker.infra.llm.client.LlmClient;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FakeLlmClient implements LlmClient {

    @Override
    public RefineResponse refine(RefineRequest request) {
        return new RefineResponse(request.targets());
    }
}
