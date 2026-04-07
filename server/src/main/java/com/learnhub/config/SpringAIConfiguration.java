package com.learnhub.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Configuration for Spring AI chat clients. Manually configures the ChatClient
 * bean from available ChatModel instances.
 */
@Configuration
public class SpringAIConfiguration {

	private static final Logger log = LoggerFactory.getLogger(SpringAIConfiguration.class);

	/**
	 * LM Studio (and some other local runtimes) incorrectly send
	 * {@code Content-Type: application/octet-stream} instead of
	 * {@code application/json}. This interceptor rewrites the Content-Type header
	 * at the HTTP transport level — before Spring AI's message converters see it —
	 * so deserialisation always succeeds regardless of what the server declares.
	 */
	@Bean
	public RestClientCustomizer octetStreamContentTypeFixCustomizer() {
		return builder -> builder.requestInterceptor((request, body, execution) -> {
			ClientHttpResponse response = execution.execute(request, body);
			if (!MediaType.APPLICATION_OCTET_STREAM.equals(response.getHeaders().getContentType())) {
				return response;
			}
			log.debug("Rewriting Content-Type application/octet-stream → application/json for {}", request.getURI());
			return new ClientHttpResponse() {
				@Override
				public HttpStatusCode getStatusCode() throws IOException {
					return response.getStatusCode();
				}
				@Override
				public String getStatusText() throws IOException {
					return response.getStatusText();
				}
				@Override
				public InputStream getBody() throws IOException {
					return response.getBody();
				}
				@Override
				public void close() {
					response.close();
				}
				@Override
				public HttpHeaders getHeaders() {
					HttpHeaders fixed = new HttpHeaders();
					fixed.putAll(response.getHeaders());
					fixed.setContentType(MediaType.APPLICATION_JSON);
					return fixed;
				}
			};
		});
	}

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
