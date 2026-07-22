package com.learnhub.config;

import java.util.List;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Configuration for Spring AI chat clients. Manually configures the ChatClient
 * bean from available ChatModel instances, and exposes the OpenAI image model
 * as the exercise image model when image generation is configured.
 */
@Configuration
public class SpringAIConfiguration {

	private static final Logger log = LoggerFactory.getLogger(SpringAIConfiguration.class);

	/**
	 * LM Studio (and some other local runtimes) incorrectly send
	 * {@code Content-Type: application/octet-stream} instead of
	 * {@code application/json}. This OkHttp interceptor rewrites the Content-Type
	 * at the HTTP transport level - before Spring AI's OpenAI client deserialises
	 * the body - so parsing always succeeds regardless of what the server declares.
	 *
	 * <p>
	 * Spring AI 2.0 drives the OpenAI models through the official OpenAI Java SDK
	 * (OkHttp) rather than Spring's {@code RestClient}, so the fix is registered
	 * via {@link OpenAiHttpClientBuilderCustomizer}. The interceptor buffers the
	 * full response body; this is safe because all chat calls here are blocking
	 * ({@code .call()}), not streaming.
	 */
	@Bean
	public OpenAiHttpClientBuilderCustomizer octetStreamContentTypeFixCustomizer() {
		return builder -> builder.interceptor(chain -> {
			Response response = chain.proceed(chain.request());
			ResponseBody body = response.body();
			MediaType contentType = body == null ? null : body.contentType();
			if (contentType == null || !"application".equalsIgnoreCase(contentType.type())
					|| !"octet-stream".equalsIgnoreCase(contentType.subtype())) {
				return response;
			}
			log.debug("Rewriting Content-Type application/octet-stream → application/json for {}",
					response.request().url());
			ResponseBody rewritten = ResponseBody.create(body.bytes(), MediaType.get("application/json"));
			return response.newBuilder().header("Content-Type", "application/json").body(rewritten).build();
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
	@Conditional(OpenAiImageConfigurationPresentCondition.class)
	public ImageModel exerciseImageModel(ApplicationContext applicationContext) {
		return applicationContext.getBean("openAiImageModel", ImageModel.class);
	}

	@Bean
	public ApplicationRunner exerciseImageModelDiagnostics(Environment environment,
			ApplicationContext applicationContext) {
		return args -> {
			if (applicationContext.containsBean("exerciseImageModel")) {
				log.info("Exercise image model enabled via OpenAI image configuration.");
				return;
			}

			String apiKey = environment.getProperty("spring.ai.openai.api-key");
			String model = environment.getProperty("spring.ai.openai.image.options.model");

			if (!StringUtils.hasText(apiKey)) {
				log.info("Exercise image model disabled: spring.ai.openai.api-key is empty.");
				return;
			}
			if (!StringUtils.hasText(model)) {
				log.info(
						"Exercise image model disabled: no image model configured (set LLM_IMAGE_MODEL_NAME / spring.ai.openai.image.options.model).");
				return;
			}
			if (!applicationContext.containsBean("openAiImageModel")) {
				log.warn(
						"Exercise image model disabled: OpenAI image model bean was not created despite apparently valid configuration. Is spring.ai.model.image=openai set?");
				return;
			}

			log.warn(
					"Exercise image model disabled for an unknown reason. OpenAI image model exists, but the alias bean was not exposed.");
		};
	}

	/**
	 * Enables the exercise image model when the shared OpenAI API key and an image
	 * model name are configured. The image model reuses the shared
	 * {@code spring.ai.openai} endpoint and key, so image generation stays disabled
	 * (rather than failing at call time) until an image model name is provided.
	 */
	static final class OpenAiImageConfigurationPresentCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String apiKey = context.getEnvironment().getProperty("spring.ai.openai.api-key");
			String model = context.getEnvironment().getProperty("spring.ai.openai.image.options.model");

			return StringUtils.hasText(apiKey) && StringUtils.hasText(model);
		}
	}
}
