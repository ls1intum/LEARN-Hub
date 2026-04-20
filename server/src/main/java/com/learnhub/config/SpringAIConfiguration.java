package com.learnhub.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

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

	@Bean(name = "exerciseImageModel")
	@Conditional(AzureImageConfigurationPresentCondition.class)
	public ImageModel exerciseImageModel(ApplicationContext applicationContext) {
		return applicationContext.getBean("azureOpenAiImageClient", ImageModel.class);
	}

	@Bean
	public ApplicationRunner exerciseImageModelDiagnostics(Environment environment,
			ApplicationContext applicationContext) {
		return args -> {
			if (applicationContext.containsBean("exerciseImageModel")) {
				log.info("Exercise image model enabled via Azure OpenAI configuration.");
				return;
			}

			String endpoint = environment.getProperty("spring.ai.azure.openai.endpoint");
			String apiKey = environment.getProperty("spring.ai.azure.openai.api-key");
			String deploymentName = environment.getProperty("spring.ai.azure.openai.image.options.deployment-name");
			String model = environment.getProperty("spring.ai.azure.openai.image.options.model");

			if (!StringUtils.hasText(endpoint)) {
				log.info("Exercise image model disabled: spring.ai.azure.openai.endpoint is empty.");
				return;
			}
			if (endpoint.contains("your-resource-name")) {
				log.info(
						"Exercise image model disabled: spring.ai.azure.openai.endpoint still uses the placeholder value '{}'.",
						endpoint);
				return;
			}
			if (!StringUtils.hasText(apiKey)) {
				log.info("Exercise image model disabled: spring.ai.azure.openai.api-key is empty.");
				return;
			}
			if (!StringUtils.hasText(deploymentName) && !StringUtils.hasText(model)) {
				log.info("Exercise image model disabled: both Azure image deployment-name and model are empty.");
				return;
			}
			if (!applicationContext.containsBean("azureOpenAiImageClient")) {
				log.warn(
						"Exercise image model disabled: Azure OpenAI image client bean was not created despite apparently valid configuration.");
				return;
			}

			log.warn(
					"Exercise image model disabled for an unknown reason. Azure image client exists, but the alias bean was not exposed.");
		};
	}

	static final class AzureImageConfigurationPresentCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String endpoint = context.getEnvironment().getProperty("spring.ai.azure.openai.endpoint");
			String apiKey = context.getEnvironment().getProperty("spring.ai.azure.openai.api-key");
			String deploymentName = context.getEnvironment()
					.getProperty("spring.ai.azure.openai.image.options.deployment-name");
			String model = context.getEnvironment().getProperty("spring.ai.azure.openai.image.options.model");

			return StringUtils.hasText(endpoint) && !endpoint.contains("your-resource-name")
					&& StringUtils.hasText(apiKey)
					&& (StringUtils.hasText(deploymentName) || StringUtils.hasText(model));
		}
	}
}
