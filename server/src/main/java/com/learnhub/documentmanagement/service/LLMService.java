package com.learnhub.documentmanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LLMService {

	private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
	private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```",
			Pattern.DOTALL);
	private static final Pattern IMAGE_PLACEHOLDER_PATTERN = Pattern
			.compile("\\[\\[IMAGE_PLACEHOLDER:\\s*(.*?)\\s*]\\s*]", Pattern.DOTALL);
	private static final Pattern IMAGE_PLACEHOLDER_WITH_ID_PATTERN = Pattern
			.compile("^id\\s*=\\s*([A-Za-z0-9_-]+)\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final String GENERATED_IMAGE_ALT_TEXT = "Generiertes Bild";
	private static final Duration IMAGE_FETCH_TIMEOUT = Duration.ofSeconds(30);
	private static final HttpClient IMAGE_FETCH_CLIENT = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final List<String> EXERCISE_IMAGE_REPLACEMENT_ORDER = List.of("exercise", "exercise_solution");
	// Strips embedded base64 image data (and their learnhub HTML comments) from
	// markdown
	// to prevent sending megabytes of image data to text/image LLMs as context.
	private static final Pattern EMBEDDED_MARKDOWN_IMAGE_PATTERN = Pattern.compile(
			"(?:<!--\\s*learnhub-image[^>]*-->\\s*)?!\\[[^\\]]*\\]\\(data:[^;)\"'\\s]+;base64,[A-Za-z0-9+/]+=*\\)",
			Pattern.DOTALL);
	private static final Pattern EMBEDDED_HTML_IMAGE_PATTERN = Pattern.compile(
			"<img\\b[^>]*\\bsrc\\s*=\\s*(?:\"data:[^\"]+;base64,[A-Za-z0-9+/=\\r\\n]+\"|'data:[^']+;base64,[A-Za-z0-9+/=\\r\\n]+'|data:[^\\s>]+;base64,[A-Za-z0-9+/=\\r\\n]+)[^>]*>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final Pattern BASE64_DATA_URI_PATTERN = Pattern
			.compile("data:[^,\\s)\"']+;base64,[A-Za-z0-9+/=\\r\\n]+", Pattern.CASE_INSENSITIVE);
	private static final int MAX_IMAGE_DESCRIPTION_CHARS = 4000;
	private static final int MAX_IMAGE_CONTEXT_CHARS = 16000;

	private final ChatClient chatClient;
	private final ImageModel exerciseImageModel;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("classpath:prompts/ActivityDataExtraction.st")
	private Resource extractionPromptResource;

	@Value("classpath:prompts/ArtikulationsschemaGeneration.st")
	private Resource artikulationsschemaPromptResource;

	@Value("classpath:prompts/DeckblattGeneration.st")
	private Resource deckblattPromptResource;

	@Value("classpath:prompts/HintergrundwissenGeneration.st")
	private Resource hintergrundwissenPromptResource;

	@Value("classpath:prompts/UebungGeneration.st")
	private Resource uebungPromptResource;

	@Value("classpath:prompts/ExerciseImageGeneration.st")
	private Resource exerciseImagePromptResource;

	@Value("classpath:prompts/TafelbildGeneration.st")
	private Resource tafelbildPromptResource;

	@Value("classpath:prompts/TafelbildImageGeneration.st")
	private Resource tafelbildImagePromptResource;

	@Autowired
	public LLMService(ChatClient chatClient,
			@Qualifier("exerciseImageModel") ObjectProvider<ImageModel> exerciseImageModelProvider) {
		this(chatClient, exerciseImageModelProvider.getIfAvailable());
	}

	public LLMService(ChatClient chatClient) {
		this(chatClient, (ImageModel) null);
	}

	LLMService(ChatClient chatClient, ImageModel exerciseImageModel) {
		if (chatClient == null) {
			throw new IllegalStateException("ChatClient is not available. Please configure a ChatModel.");
		}
		this.chatClient = chatClient;
		this.exerciseImageModel = exerciseImageModel;
	}

	// Compact JSON with ~20 short fields — 800 tokens is more than sufficient.
	private static final int MAX_TOKENS_EXTRACTION = 4000;

	// Exactly 6 AVIVA+ rows with concrete per-cell content.
	private static final int MAX_TOKENS_LESSON_PLAN = 10000;

	// Structured cover-page markdown (4 sections, numbered list, bullet lists).
	private static final int MAX_TOKENS_COVER_SHEET = 8000;

	// Three free-text sections of teacher background knowledge.
	private static final int MAX_TOKENS_BACKGROUND_KNOWLEDGE = 10000;

	// Exercise sheet + matching solution sheet in one response.
	private static final int MAX_TOKENS_EXERCISE = 16000;
	private static final int MAX_TOKENS_BOARD_IMAGE = 10000;

	public Map<String, Object> extractActivityData(String pdfText) {

		String promptText = new PromptTemplate(extractionPromptResource).render(Map.of("pdfText", pdfText));

		try {
			String responseText = callLlm(promptText, MAX_TOKENS_EXTRACTION);

			logger.debug("LLM Response: {}", responseText);

			String jsonPayload = extractJsonPayload(responseText);

			// Parse JSON response
			return objectMapper.readValue(jsonPayload, new TypeReference<Map<String, Object>>() {
			});
		} catch (JsonProcessingException e) {
			throw new RuntimeException("LLM returned invalid JSON: " + e.getOriginalMessage(), e);
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract activity data from PDF: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate or extract an Artikulationsschema from PDF text. If the PDF already
	 * contains a schema, extract and normalize it. Otherwise, infer a fitting
	 * schema from the teaching material. Returns markdown text.
	 *
	 * @param pdfText
	 *            extracted text from the PDF
	 * @param metadata
	 *            user-adjusted activity metadata to inform the schema
	 */
	public String generateLessonPlan(String pdfText, Map<String, Object> metadata) {
		String metadataSection = buildMetadataSection(metadata);
		String promptText = new PromptTemplate(artikulationsschemaPromptResource)
				.render(Map.of("metadataSection", metadataSection, "pdfText", pdfText));

		try {
			String responseText = callLlm(promptText, MAX_TOKENS_LESSON_PLAN);

			logger.debug("LLM Artikulationsschema Response: {}", responseText);

			return extractMarkdownPayload(responseText);
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate Artikulationsschema: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate a Deckblatt (cover page) from PDF text. Returns markdown text.
	 *
	 * @param pdfText
	 *            extracted text from the PDF
	 * @param metadata
	 *            user-adjusted activity metadata to inform the generation
	 */
	public String generateCoverSheet(String pdfText, Map<String, Object> metadata) {
		String metadataSection = buildMetadataSection(metadata);
		String promptText = new PromptTemplate(deckblattPromptResource)
				.render(Map.of("metadataSection", metadataSection, "pdfText", pdfText));

		try {
			String responseText = callLlm(promptText, MAX_TOKENS_COVER_SHEET);

			logger.debug("LLM Deckblatt Response: {}", responseText);

			return extractMarkdownPayload(responseText);
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate Deckblatt: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate Hintergrundwissen (background knowledge) from PDF text. Returns
	 * markdown text.
	 *
	 * @param pdfText
	 *            extracted text from the PDF
	 * @param metadata
	 *            user-adjusted activity metadata to inform the generation
	 */
	public String generateBackgroundKnowledge(String pdfText, Map<String, Object> metadata) {
		String metadataSection = buildMetadataSection(metadata);
		String promptText = new PromptTemplate(hintergrundwissenPromptResource)
				.render(Map.of("metadataSection", metadataSection, "pdfText", pdfText));

		try {
			String responseText = callLlm(promptText, MAX_TOKENS_BACKGROUND_KNOWLEDGE);

			logger.debug("LLM Hintergrundwissen Response: {}", responseText);

			return extractMarkdownPayload(responseText);
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate Hintergrundwissen: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate a Tafelbild (Hefteintrag) from the provided Artikulationsschema.
	 * Returns markdown with embedded images, combining text sections and at least
	 * one illustrative image.
	 *
	 * @param lessonPlan
	 *            previously generated Artikulationsschema markdown
	 * @param metadata
	 *            activity metadata (ageMin/ageMax drive language calibration)
	 */
	public String generateBoardImageMarkdown(String lessonPlan, Map<String, Object> metadata) {
		String normalizedLessonPlan = stripEmbeddedImages(lessonPlan == null ? "" : lessonPlan.trim());
		String metadataSection = buildMetadataSection(metadata);
		String promptText = new PromptTemplate(tafelbildPromptResource)
				.render(Map.of("artikulationsschema", normalizedLessonPlan, "metadataSection", metadataSection));

		try {
			String responseText = callLlm(promptText, MAX_TOKENS_BOARD_IMAGE);
			logger.debug("LLM Tafelbild Response: {}", responseText);
			String markdown = extractMarkdownPayload(responseText);
			Map<String, String> imageCache = new HashMap<>();
			return replaceImagePlaceholders(markdown, imageCache, new HashMap<>(),
					placeholder -> generateBoardImageIllustrationMarkdownSafely(placeholder, normalizedLessonPlan));
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate Tafelbild: " + e.getMessage(), e);
		}
	}

	private String generateBoardImageIllustrationMarkdownSafely(ImagePlaceholder placeholder, String lessonPlan) {
		if (exerciseImageModel == null) {
			logger.warn("Skipping Tafelbild image placeholder because no image model is configured");
			return placeholder.marker();
		}
		try {
			String prompt = buildBoardImageIllustrationPrompt(placeholder.description(), lessonPlan);
			ImageResponse response = exerciseImageModel.call(new ImagePrompt(prompt));
			if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
				throw new IllegalStateException("Image model returned an empty response");
			}
			String altText = StringUtils.hasText(placeholder.id()) ? placeholder.id() : GENERATED_IMAGE_ALT_TEXT;
			String comment = buildImageComment(placeholder.id(), placeholder.description());
			Image image = response.getResult().getOutput();
			if (StringUtils.hasText(image.getB64Json())) {
				return comment + "\n![" + altText + "](data:image/png;base64," + image.getB64Json() + ")";
			}
			if (StringUtils.hasText(image.getUrl())) {
				return comment + "\n" + toMarkdownImageFromUrl(image.getUrl(), altText);
			}
			throw new IllegalStateException("Image model returned neither base64 nor URL");
		} catch (Exception e) {
			logger.warn("Failed to generate Tafelbild image for '{}': {}", placeholder.description(), e.getMessage());
			return placeholder.marker();
		}
	}

	String buildBoardImageIllustrationPrompt(String description, String contextText) {
		String normalizedDescription = truncateForImagePrompt(
				stripEmbeddedImages(description == null ? "" : description.trim()), MAX_IMAGE_DESCRIPTION_CHARS,
				"image description");
		String normalizedContextText = truncateForImagePrompt(
				stripEmbeddedImages(contextText == null ? "" : contextText.trim()), MAX_IMAGE_CONTEXT_CHARS,
				"board_image context");
		return new PromptTemplate(tafelbildImagePromptResource)
				.render(Map.of("description", normalizedDescription, "contextText", normalizedContextText));
	}

	/**
	 * Generate an age-appropriate exercise sheet and its matching solution in a
	 * single LLM call, guaranteeing the two documents correspond exactly.
	 *
	 * @param pdfText
	 *            extracted text from the PDF
	 * @param metadata
	 *            user-adjusted activity metadata (ageMin/ageMax drive difficulty)
	 * @return map with keys "exercise" and "exercise_solution", each containing
	 *         markdown
	 */
	public Map<String, String> generateExerciseAndSolution(String pdfText, Map<String, Object> metadata) {
		String metadataSection = buildMetadataSection(metadata);
		String promptText = new PromptTemplate(uebungPromptResource)
				.render(Map.of("metadataSection", metadataSection, "pdfText", pdfText));

		try {
			String responseText = callLlm(promptText, MAX_TOKENS_EXERCISE);

			logger.debug("LLM Uebung Response: {}", responseText);

			Map<String, String> generatedMarkdowns = extractExercisePayload(responseText);
			String imageContext = buildExerciseImageContext(generatedMarkdowns);
			return replaceExerciseImagePlaceholders(generatedMarkdowns, imageContext);
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate Übung and Lösung: " + e.getMessage(), e);
		}
	}

	/** Standard LLM call — text only, uses the default model. */
	private String callLlm(String promptText, int maxTokens) {
		// Spring AI 2.0's ChatClient.options(..) takes a ChatOptions.Builder, not a
		// built ChatOptions instance.
		return chatClient.prompt().user(promptText).options(OpenAiChatOptions.builder().maxTokens(maxTokens)).call()
				.content();
	}

	// Accept both styles: with ===_END=== delimiters or without (content
	// delimited by the next ===_START=== marker or end of string).
	private static final Pattern UEBUNG_PATTERN = Pattern
			.compile("===UEBUNG_START===\\s*(.*?)\\s*(?:===UEBUNG_END===|===LOESUNG_START===)", Pattern.DOTALL);
	private static final Pattern LOESUNG_PATTERN = Pattern
			.compile("===LOESUNG_START===\\s*(.*?)\\s*(?:===LOESUNG_END===\\s*)?$", Pattern.DOTALL);

	private Map<String, String> extractExercisePayload(String rawResponse) {
		if (rawResponse == null || rawResponse.trim().isEmpty()) {
			throw new IllegalStateException("LLM returned an empty response");
		}

		Matcher exerciseMatcher = UEBUNG_PATTERN.matcher(rawResponse);
		Matcher solutionMatcher = LOESUNG_PATTERN.matcher(rawResponse);

		if (!exerciseMatcher.find() || !solutionMatcher.find()) {
			throw new IllegalStateException(
					"LLM response missing required delimiters ===UEBUNG_START=== / ===LOESUNG_START===");
		}

		Map<String, String> result = new LinkedHashMap<>();
		result.put("exercise", exerciseMatcher.group(1).trim());
		result.put("exercise_solution", solutionMatcher.group(1).trim());
		return result;
	}

	Map<String, String> replaceExerciseImagePlaceholders(Map<String, String> markdowns, String pdfText) {
		if (markdowns == null || markdowns.isEmpty()) {
			return markdowns;
		}

		Map<String, String> imageMarkdownCache = new HashMap<>();
		Map<String, String> imageDescriptionCache = new HashMap<>();
		Map<String, String> replacedMarkdowns = new LinkedHashMap<>();

		for (String key : EXERCISE_IMAGE_REPLACEMENT_ORDER) {
			if (markdowns.containsKey(key)) {
				replacedMarkdowns.put(key, replaceImagePlaceholders(markdowns.get(key), imageMarkdownCache,
						imageDescriptionCache, pdfText));
			}
		}
		for (Map.Entry<String, String> entry : markdowns.entrySet()) {
			if (replacedMarkdowns.containsKey(entry.getKey())) {
				continue;
			}
			replacedMarkdowns.put(entry.getKey(),
					replaceImagePlaceholders(entry.getValue(), imageMarkdownCache, imageDescriptionCache, pdfText));
		}

		return replacedMarkdowns;
	}

	String buildExerciseImageContext(Map<String, String> generatedMarkdowns) {
		if (generatedMarkdowns == null || generatedMarkdowns.isEmpty()) {
			return "";
		}
		String exerciseMarkdown = generatedMarkdowns.getOrDefault("exercise", "").trim();
		String solutionMarkdown = generatedMarkdowns.getOrDefault("exercise_solution", "").trim();
		if (!StringUtils.hasText(exerciseMarkdown) && !StringUtils.hasText(solutionMarkdown)) {
			return "";
		}

		StringBuilder context = new StringBuilder();
		if (StringUtils.hasText(exerciseMarkdown)) {
			context.append("Generiertes Übungsblatt:\n").append(exerciseMarkdown.trim());
		}
		if (StringUtils.hasText(solutionMarkdown)) {
			if (context.length() > 0) {
				context.append("\n\n");
			}
			context.append("Generiertes Lösungsblatt:\n").append(solutionMarkdown.trim());
		}
		return context.toString();
	}

	String replaceImagePlaceholders(String markdown, Map<String, String> imageMarkdownCache, String pdfText) {
		return replaceImagePlaceholders(markdown, imageMarkdownCache, new HashMap<>(),
				placeholder -> generateImageMarkdownSafely(placeholder, pdfText));
	}

	String replaceImagePlaceholders(String markdown, Map<String, String> imageMarkdownCache,
			Map<String, String> imageDescriptionCache, String pdfText) {
		return replaceImagePlaceholders(markdown, imageMarkdownCache, imageDescriptionCache,
				placeholder -> generateImageMarkdownSafely(placeholder, pdfText));
	}

	private String replaceImagePlaceholders(String markdown, Map<String, String> imageMarkdownCache,
			Map<String, String> imageDescriptionCache, Function<ImagePlaceholder, String> imageGenerator) {
		if (!StringUtils.hasText(markdown)) {
			return markdown;
		}

		Matcher matcher = IMAGE_PLACEHOLDER_PATTERN.matcher(markdown);
		if (!matcher.find()) {
			return markdown;
		}

		matcher.reset();
		StringBuffer replaced = new StringBuffer();
		while (matcher.find()) {
			ImagePlaceholder placeholder = parseImagePlaceholder(matcher.group(1));
			String existingDescription = imageDescriptionCache.putIfAbsent(placeholder.cacheKey(),
					placeholder.description());
			if (placeholder.id() != null && existingDescription != null
					&& !existingDescription.equals(placeholder.description())) {
				logger.info("Reusing image id '{}' with a different description. Keeping the first generated image.",
						placeholder.id());
			}
			String imageMarkdown = imageMarkdownCache.computeIfAbsent(placeholder.cacheKey(),
					key -> imageGenerator.apply(placeholder));
			matcher.appendReplacement(replaced, Matcher.quoteReplacement(imageMarkdown));
		}
		matcher.appendTail(replaced);
		return replaced.toString();
	}

	public String generateImageMarkdown(String id, String description, String contextText) {
		if (exerciseImageModel == null) {
			throw new IllegalStateException("Exercise image model is not configured");
		}

		ImageResponse response = exerciseImageModel
				.call(new ImagePrompt(buildExerciseImagePrompt(description, contextText)));
		if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
			throw new IllegalStateException("Image model returned an empty response");
		}

		String altText = StringUtils.hasText(id) ? id : GENERATED_IMAGE_ALT_TEXT;
		String comment = buildImageComment(id, description);
		Image image = response.getResult().getOutput();
		if (StringUtils.hasText(image.getB64Json())) {
			return comment + "\n![" + altText + "](data:image/png;base64," + image.getB64Json() + ")";
		}
		if (StringUtils.hasText(image.getUrl())) {
			return comment + "\n" + toMarkdownImageFromUrl(image.getUrl(), altText);
		}
		throw new IllegalStateException("Image model response contained neither base64 image data nor a URL");
	}

	public String generateBoardImageIllustrationMarkdown(String id, String description, String contextText) {
		if (exerciseImageModel == null) {
			throw new IllegalStateException("Exercise image model is not configured");
		}

		ImageResponse response = exerciseImageModel
				.call(new ImagePrompt(buildBoardImageIllustrationPrompt(description, contextText)));
		if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
			throw new IllegalStateException("Image model returned an empty response");
		}

		String altText = StringUtils.hasText(id) ? id : GENERATED_IMAGE_ALT_TEXT;
		String comment = buildImageComment(id, description);
		Image image = response.getResult().getOutput();
		if (StringUtils.hasText(image.getB64Json())) {
			return comment + "\n![" + altText + "](data:image/png;base64," + image.getB64Json() + ")";
		}
		if (StringUtils.hasText(image.getUrl())) {
			return comment + "\n" + toMarkdownImageFromUrl(image.getUrl(), altText);
		}
		throw new IllegalStateException("Image model response contained neither base64 image data nor a URL");
	}

	private String buildImageComment(String id, String description) {
		String safeId = id != null ? id : "";
		String safeDesc = description != null ? description.replace("-->", "-- >") : "";
		return "<!-- learnhub-image:id=" + safeId + "; prompt=" + safeDesc + " -->";
	}

	private String generateImageMarkdownSafely(ImagePlaceholder placeholder, String pdfText) {
		if (exerciseImageModel == null) {
			logger.warn("Skipping image placeholder replacement because no exercise image model is configured");
			return placeholder.marker();
		}

		try {
			return generateImageMarkdown(placeholder.id(), placeholder.description(), pdfText);
		} catch (Exception e) {
			logger.warn("Failed to replace image placeholder '{}': {}", placeholder.description(), e.getMessage());
			return placeholder.marker();
		}
	}

	static String stripEmbeddedImages(String markdown) {
		if (markdown == null || markdown.isEmpty()) {
			return markdown;
		}
		String withoutMarkdownImages = EMBEDDED_MARKDOWN_IMAGE_PATTERN.matcher(markdown).replaceAll("[Bild]");
		String withoutHtmlImages = EMBEDDED_HTML_IMAGE_PATTERN.matcher(withoutMarkdownImages).replaceAll("[Bild]");
		return BASE64_DATA_URI_PATTERN.matcher(withoutHtmlImages).replaceAll("[Bilddaten entfernt]");
	}

	String buildExerciseImagePrompt(String description, String contextText) {
		String normalizedDescription = truncateForImagePrompt(
				stripEmbeddedImages(description == null ? "" : description.trim()), MAX_IMAGE_DESCRIPTION_CHARS,
				"image description");
		String normalizedContextText = truncateForImagePrompt(
				stripEmbeddedImages(contextText == null ? "" : contextText.trim()), MAX_IMAGE_CONTEXT_CHARS,
				"exercise context");
		if (exerciseImagePromptResource == null) {
			throw new IllegalStateException("Exercise image prompt resource is not configured");
		}
		if (!exerciseImagePromptResource.exists() || !exerciseImagePromptResource.isReadable()) {
			throw new IllegalStateException("Exercise image prompt resource is not readable");
		}
		return new PromptTemplate(exerciseImagePromptResource)
				.render(Map.of("description", normalizedDescription, "contextText", normalizedContextText));
	}

	private String truncateForImagePrompt(String value, int maxChars, String fieldName) {
		if (value == null || value.length() <= maxChars) {
			return value;
		}

		logger.info("Truncating exercise image {} from {} to {} characters", fieldName, value.length(), maxChars);
		int headLength = Math.max(0, (maxChars * 3) / 4);
		int tailLength = Math.max(0, maxChars - headLength);
		return value.substring(0, headLength).trim() + "\n\n[... omitted " + (value.length() - maxChars)
				+ " characters ...]\n\n" + value.substring(value.length() - tailLength).trim();
	}

	private String toMarkdownImageFromUrl(String imageUrl, String altText) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl)).GET().timeout(IMAGE_FETCH_TIMEOUT)
					.build();
			HttpResponse<byte[]> response = IMAGE_FETCH_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IllegalStateException("Failed to download generated image: HTTP " + response.statusCode());
			}

			String mimeType = response.headers().firstValue("Content-Type").map(value -> value.split(";", 2)[0].trim())
					.filter(StringUtils::hasText).orElseGet(() -> inferImageMimeType(imageUrl));
			String base64 = Base64.getEncoder().encodeToString(response.body());
			return "![" + altText + "](data:" + mimeType + ";base64," + base64 + ")";
		} catch (Exception e) {
			throw new IllegalStateException("Failed to download generated image from URL", e);
		}
	}

	private String inferImageMimeType(String imageUrl) {
		String normalizedUrl = imageUrl == null ? "" : imageUrl.toLowerCase();
		if (normalizedUrl.contains(".jpg") || normalizedUrl.contains(".jpeg")) {
			return "image/jpeg";
		}
		if (normalizedUrl.contains(".webp")) {
			return "image/webp";
		}
		if (normalizedUrl.contains(".gif")) {
			return "image/gif";
		}
		return "image/png";
	}

	private ImagePlaceholder parseImagePlaceholder(String rawPlaceholderContent) {
		String normalized = rawPlaceholderContent == null ? "" : rawPlaceholderContent.trim();
		while (normalized.endsWith("]")) {
			normalized = normalized.substring(0, normalized.length() - 1).trim();
		}
		Matcher matcher = IMAGE_PLACEHOLDER_WITH_ID_PATTERN.matcher(normalized);
		if (!matcher.matches()) {
			return new ImagePlaceholder("desc:" + normalized, null, normalized,
					buildImagePlaceholderMarker(null, normalized));
		}

		String id = matcher.group(1).trim().toLowerCase(Locale.ROOT);
		String description = matcher.group(2).trim();
		return new ImagePlaceholder("id:" + id, id, description, buildImagePlaceholderMarker(id, description));
	}

	private String buildImagePlaceholderMarker(String id, String description) {
		if (StringUtils.hasText(id)) {
			return "[[IMAGE_PLACEHOLDER:id=" + id + ": " + description + "]]";
		}
		return "[[IMAGE_PLACEHOLDER: " + description + "]]";
	}

	private record ImagePlaceholder(String cacheKey, String id, String description, String marker) {
	}

	private String extractJsonPayload(String rawResponse) {
		if (rawResponse == null || rawResponse.trim().isEmpty()) {
			throw new IllegalStateException("LLM returned an empty response");
		}

		String trimmedResponse = rawResponse.trim();

		Matcher codeBlockMatch = JSON_CODE_BLOCK_PATTERN.matcher(trimmedResponse);
		if (codeBlockMatch.find()) {
			return codeBlockMatch.group(1).trim();
		}

		// Some models prepend thinking text before the final JSON output.
		int jsonStart = trimmedResponse.indexOf('{');
		int jsonEnd = trimmedResponse.lastIndexOf('}');
		if (jsonStart >= 0 && jsonEnd > jsonStart) {
			return trimmedResponse.substring(jsonStart, jsonEnd + 1).trim();
		}

		throw new IllegalStateException("LLM response does not contain a JSON object");
	}

	private String buildMetadataSection(Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return "";
		}
		StringBuilder metadataSection = new StringBuilder();
		metadataSection.append("\n\nMETADATEN DER AKTIVITÄT (von der Lehrkraft bestätigt):\n");
		if (metadata.containsKey("name")) {
			metadataSection.append("- Name: ").append(metadata.get("name")).append("\n");
		}
		if (metadata.containsKey("description")) {
			metadataSection.append("- Beschreibung: ").append(metadata.get("description")).append("\n");
		}
		if (metadata.containsKey("ageMin") || metadata.containsKey("ageMax")) {
			metadataSection.append("- Altersbereich: ").append(metadata.getOrDefault("ageMin", "?")).append("-")
					.append(metadata.getOrDefault("ageMax", "?")).append("\n");
		}
		if (metadata.containsKey("format")) {
			metadataSection.append("- Format: ").append(metadata.get("format")).append("\n");
		}
		if (metadata.containsKey("bloomLevel")) {
			metadataSection.append("- Bloom-Stufe: ").append(metadata.get("bloomLevel")).append("\n");
		}
		if (metadata.containsKey("durationMinMinutes")) {
			metadataSection.append("- Dauer (min): ").append(metadata.get("durationMinMinutes"));
			if (metadata.containsKey("durationMaxMinutes")) {
				metadataSection.append("-").append(metadata.get("durationMaxMinutes"));
			}
			metadataSection.append(" Minuten\n");
		}
		if (metadata.containsKey("resourcesNeeded")) {
			metadataSection.append("- Benötigte Materialien: ").append(metadata.get("resourcesNeeded")).append("\n");
		}
		if (metadata.containsKey("topics")) {
			metadataSection.append("- Themen: ").append(metadata.get("topics")).append("\n");
		}
		if (metadata.containsKey("mentalLoad")) {
			metadataSection.append("- Kognitive Belastung: ").append(metadata.get("mentalLoad")).append("\n");
		}
		if (metadata.containsKey("physicalEnergy")) {
			metadataSection.append("- Körperliche Aktivität: ").append(metadata.get("physicalEnergy")).append("\n");
		}
		if (metadata.containsKey("source")) {
			metadataSection.append("- Quelle: ").append(metadata.get("source")).append("\n");
		}
		metadataSection
				.append("\nVerwende diese Metadaten für Klassenstufe, Dauer, Thema und die Spalte Medien/Material.\n");
		return metadataSection.toString();
	}

	/**
	 * Extract clean markdown from LLM response, stripping any wrapper code blocks.
	 */
	private String extractMarkdownPayload(String rawResponse) {
		if (rawResponse == null || rawResponse.trim().isEmpty()) {
			throw new IllegalStateException("LLM returned an empty response");
		}

		String trimmed = rawResponse.trim();

		// Remove markdown code block wrappers if present
		if (trimmed.startsWith("```markdown")) {
			trimmed = trimmed.substring("```markdown".length());
			if (trimmed.endsWith("```")) {
				trimmed = trimmed.substring(0, trimmed.length() - 3);
			}
			return trimmed.trim();
		}

		if (trimmed.startsWith("```md")) {
			trimmed = trimmed.substring("```md".length());
			if (trimmed.endsWith("```")) {
				trimmed = trimmed.substring(0, trimmed.length() - 3);
			}
			return trimmed.trim();
		}

		if (trimmed.startsWith("```")) {
			trimmed = trimmed.substring(3);
			if (trimmed.endsWith("```")) {
				trimmed = trimmed.substring(0, trimmed.length() - 3);
			}
			return trimmed.trim();
		}

		return trimmed;
	}
}
