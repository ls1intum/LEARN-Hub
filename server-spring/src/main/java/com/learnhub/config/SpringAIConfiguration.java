package com.learnhub.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Generic configuration for Spring AI chat clients.
 * This configuration is model-agnostic and uses whatever ChatModel is available
 * (e.g., Ollama, OpenAI, Azure OpenAI) based on application properties.
 */
@Configuration
public class SpringAIConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SpringAIConfiguration.class);

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
