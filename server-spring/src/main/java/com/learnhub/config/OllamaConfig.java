package com.learnhub.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Value("${spring.ai.ollama.chat.model}")
    private String model;

    @Value("${ollama.api.key}")
    private String apiKey;

    @Value("${spring.ai.ollama.chat.options.temperature:0.1}")
    private Double temperature;

    @Value("${spring.ai.ollama.chat.options.num-predict:2048}")
    private Integer numPredict;

    @Bean
    public OllamaChatModel ollamaChatModel() {
        // Create RestClient with Authorization header for API key authentication
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(baseUrl);

        // Add Authorization header with Bearer token if API key is provided
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("REDACTED")) {
            restClientBuilder.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        RestClient restClient = restClientBuilder.build();

        // Create OllamaApi with custom RestClient
        OllamaApi ollamaApi = new OllamaApi(baseUrl, restClient);

        // Configure options
        OllamaOptions options = OllamaOptions.builder()
                .withModel(model)
                .withTemperature(temperature.floatValue())
                .withNumPredict(numPredict)
                .build();

        // Create and return OllamaChatModel
        return new OllamaChatModel(ollamaApi, options);
    }
}
