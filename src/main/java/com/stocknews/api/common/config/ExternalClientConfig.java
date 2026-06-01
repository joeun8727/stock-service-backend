package com.stocknews.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocknews.api.client.financial.FinnhubFinancialClient;
import com.stocknews.api.client.llm.GeminiClient;
import com.stocknews.api.client.macro.FredClient;
import com.stocknews.api.client.news.FinnhubNewsClient;
import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class ExternalClientConfig {

    private final ClientHttpRequestFactory httpRequestFactory;
    private final ApiCallLogRepository apiCallLogRepository;

    @Bean
    public FinnhubNewsClient finnhubNewsClient(
            @Value("${external.finnhub.base-url}") String baseUrl,
            @Value("${external.finnhub.api-key}") String apiKey
    ) {
        RestClient restClient = RestClient.builder()
                .requestFactory(httpRequestFactory)
                .baseUrl(baseUrl)
                .defaultHeader("X-Finnhub-Token", apiKey)
                .build();
        return new FinnhubNewsClient(restClient, apiCallLogRepository);
    }

    @Bean
    public FinnhubFinancialClient finnhubFinancialClient(
            @Value("${external.finnhub.base-url}") String baseUrl,
            @Value("${external.finnhub.api-key}") String apiKey
    ) {
        RestClient restClient = RestClient.builder()
                .requestFactory(httpRequestFactory)
                .baseUrl(baseUrl)
                .defaultHeader("X-Finnhub-Token", apiKey)
                .build();
        return new FinnhubFinancialClient(restClient, apiCallLogRepository);
    }

    @Bean
    public FredClient fredClient(
            @Value("${external.fred.base-url}") String baseUrl,
            @Value("${external.fred.api-key}") String apiKey
    ) {
        RestClient restClient = RestClient.builder()
                .requestFactory(httpRequestFactory)
                .baseUrl(baseUrl)
                .build();
        return new FredClient(restClient, apiKey, apiCallLogRepository);
    }

    @Bean
    public GeminiClient geminiClient(
            @Value("${external.gemini.base-url}") String baseUrl,
            @Value("${external.gemini.api-key}") String apiKey,
            @Value("${external.gemini.model}") String model,
            ObjectMapper objectMapper
    ) {
        RestClient restClient = RestClient.builder()
                .requestFactory(httpRequestFactory)
                .baseUrl(baseUrl)
                .build();
        return new GeminiClient(restClient, apiKey, model, objectMapper, apiCallLogRepository);
    }
}
