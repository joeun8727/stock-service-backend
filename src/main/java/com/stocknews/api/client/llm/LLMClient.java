package com.stocknews.api.client.llm;

public interface LLMClient {

    LLMAnalysis analyze(String headline, String snippet);
}
