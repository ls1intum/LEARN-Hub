package com.learnhub.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.api.key:}")
    private String apiKey;

    @Bean
    public OllamaChatModel ollamaChatModel() {
        // Create RestClient with Authorization header for API key authentication
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(baseUrl);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(baseUrl);

        // Add Authorization header with Bearer token if API key is provided
        if (apiKey != null && !apiKey.isEmpty()) {
            restClientBuilder.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        // Create OllamaApi with custom RestClient
        OllamaApi ollamaApi = new OllamaApi(baseUrl, restClientBuilder, webClientBuilder);

        // Create and return OllamaChatModel
        return OllamaChatModel.builder().ollamaApi(ollamaApi).build();
    }
}
