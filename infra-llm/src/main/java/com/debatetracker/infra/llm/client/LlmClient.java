package com.debatetracker.infra.llm.client;

public interface LlmClient {

    RefineResponse refine(RefineRequest request);

    ExtractAgendaResponse extract(ExtractAgendaRequest request);
}
