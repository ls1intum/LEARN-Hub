package com.learnhub.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Spring AI chat clients.
 * Supports Ollama-compatible APIs with optional Bearer token authentication.
 */
@Configuration
public class SpringAIConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SpringAIConfiguration.class);

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Value("${llm.api.key:}")
    private String apiKey;

    /**
     * Creates a custom OllamaChatModel with API key authentication support.
     * This bean is only created when llm.api.key is configured.
     *
     * @return OllamaChatModel with authentication headers
     */
    @Bean
    @ConditionalOnProperty(name = "llm.api.key")
    public OllamaChatModel ollamaChatModelWithAuth(
            @Value("${spring.ai.ollama.chat.options.model}") String model,
            @Value("${spring.ai.ollama.chat.options.temperature}") Double temperature,
            @Value("${spring.ai.ollama.chat.options.num-predict}") Integer numPredict) {

        RestClient.Builder restClientBuilder = RestClient.builder();
        WebClient.Builder webClientBuilder = WebClient.builder();

        log.info("Configuring LLM API with Bearer token authentication");
        restClientBuilder.defaultHeader("Authorization", "Bearer " + apiKey);
        webClientBuilder.defaultHeader("Authorization", "Bearer " + apiKey);

        OllamaApi ollamaApi = new OllamaApi(baseUrl, restClientBuilder, webClientBuilder);

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        org.springframework.ai.ollama.api.OllamaOptions.builder()
                                .model(model)
                                .temperature(temperature)
                                .numPredict(numPredict)
                                .build())
                .build();
    }

    /**
     * Creates a ChatClient from available ChatModel beans.
     * Uses the first available model if multiple are configured.
     * This bean is only created if at least one ChatModel is available.
     *
     * @param chatModels list of available chat models (auto-configured by Spring AI)
     * @return a configured ChatClient
     */
    @Bean
    @ConditionalOnBean(ChatModel.class)
    public ChatClient chatClient(List<ChatModel> chatModels) {
        for (ChatModel model : chatModels) {
            if (model.getDefaultOptions() != null) {
                log.info(
                    "Found Chat Model: {} with temperature: {}",
                    model.getDefaultOptions().getModel(),
                    model.getDefaultOptions().getTemperature()
                );
            } else {
                log.info("Found Chat Model: {} (no default options)", model.getClass().getSimpleName());
            }
        }

        ChatModel chatModel = chatModels.getFirst();
        return ChatClient.builder(chatModel).build();
    }
}
