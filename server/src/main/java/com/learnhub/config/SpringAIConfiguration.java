package com.learnhub.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AI chat clients. Manually configures the ChatClient
 * bean from available ChatModel instances.
 */
@Configuration
public class SpringAIConfiguration {

	private static final Logger log = LoggerFactory.getLogger(SpringAIConfiguration.class);

	/**
	 * Creates a ChatClient from the available ChatModel beans.
	 *
	 * @param chatModels
	 *            list of available chat models (may be empty if none configured)
	 * @return a configured ChatClient, or null if no model is available
	 */
	@Bean
	public ChatClient chatClient(List<ChatModel> chatModels) {
		if (chatModels == null || chatModels.isEmpty()) {
			log.warn("No ChatModel found. LLM features will be disabled.");
			return null;
		}
		for (ChatModel model : chatModels) {
			log.info("Found Chat Model: {} with temperature: {}", model.getDefaultOptions().getModel(),
					model.getDefaultOptions().getTemperature());
		}
		ChatModel chatModel = chatModels.getFirst();
		return ChatClient.builder(chatModel).build();
	}
}
