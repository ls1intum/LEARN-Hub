package com.learnhub.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@Configuration
public class OllamaConfig {

    private static final Logger logger = LoggerFactory.getLogger(OllamaConfig.class);

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.api.key:}")
    private String apiKey;

    @Bean
    public OllamaChatModel ollamaChatModel() {
        // Create RestClient with Authorization header for API key authentication
        RestClient.Builder restClientBuilder = RestClient.builder()
                // .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    logger.info("Ollama request: {} {}", request.getMethod(), request.getURI());
                    logger.info("Headers: {}", request.getHeaders());
                    logger.info("Body: {}", new String(body, StandardCharsets.UTF_8));
                    return execution.execute(request, body);
                });
        WebClient.Builder webClientBuilder = WebClient.builder()
                // .baseUrl(baseUrl)
                .filter((request, next) -> {
                    logger.info("Ollama WebClient request: {} {}", request.method(), request.url());
                    logger.info("Headers: {}", request.headers());
                    return next.exchange(request);
                });

        // Add Authorization header with Bearer token if API key is provided
        if (apiKey != null && !apiKey.isEmpty()) {
            restClientBuilder.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        // Create OllamaApi with custom RestClient
        OllamaApi ollamaApi = new OllamaApi(baseUrl, restClientBuilder, webClientBuilder);

        // Create and return OllamaChatModel
        return OllamaChatModel.builder().ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaOptions.builder().model("qwen3:30b-a3b").temperature(0.1).numPredict(2048).build())
                .build();
    }
}
