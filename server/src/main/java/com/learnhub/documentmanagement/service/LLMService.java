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
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
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
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	private static final List<String> EXERCISE_IMAGE_REPLACEMENT_ORDER = List.of("uebung", "uebung_loesung");
	// Strips embedded base64 image data (and their learnhub HTML comments) from markdown
	// to prevent sending megabytes of image data to text/image LLMs as context.
	private static final Pattern EMBEDDED_IMAGE_PATTERN = Pattern.compile(
			"(?:<!--\\s*learnhub-image[^>]*-->\\s*)?!\\[[^\\]]*\\]\\(data:[^;)\"'\\s]+;base64,[A-Za-z0-9+/]+=*\\)",
			Pattern.DOTALL);

	@Value("${llm.visual.model:}")
	private String visualModelName;

	@Value("${llm.visual.max-tokens:8000}")
	private int visualMaxTokens;

	private final ChatClient chatClient;
	private final ImageModel exerciseImageModel;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final String DEFAULT_EXERCISE_IMAGE_PROMPT_TEMPLATE = """
			You generate images for school exercise sheets.

			These images are not decorative. They explain tasks and must be accurate enough that the exercise still makes sense when learners look at the image.

			Follow these rules for every generated image:
			- Treat the provided description as task-critical context.
			- Preserve all important factual details from the description exactly.
			- Make spatial relationships, counts, labels, symbols, paths, positions, and visual distinctions unambiguous.
			- If the description mentions specific objects, icons, text labels, arrows, routes, grids, numbers, start/goal fields, or left/right/top/bottom placement, include them exactly as described.
			- Do not invent extra task elements that could confuse the learner.
			- Do not add hidden hints, extra solutions, teacher-only annotations, or highlighted answers unless the description explicitly asks for them.
			- Prefer clean, readable composition over artistic flair. Clarity and correctness matter more than style.
			- Ensure the final image is coherent, classroom-safe, and easy to understand in the context of an exercise sheet.

			Use the following generated exercise and solution text as additional context for factual accuracy. The image must match this generated worksheet content exactly:

			{contextText}

			Use this exact exercise-image specification:

			{description}
			""";

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
	private static final int MAX_TOKENS_ARTIKULATIONSSCHEMA = 10000;

	// Structured cover-page markdown (4 sections, numbered list, bullet lists).
	private static final int MAX_TOKENS_DECKBLATT = 8000;

	// Three free-text sections of teacher background knowledge.
	private static final int MAX_TOKENS_HINTERGRUNDWISSEN = 10000;

	// Exercise sheet + matching solution sheet in one response.
	private static final int MAX_TOKENS_UEBUNG = 16000;

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
	public String generateArtikulationsschema(String pdfText, Map<String, Object> metadata) {
		String metadataSection = buildMetadataSection(metadata);
		String promptText = new PromptTemplate(artikulationsschemaPromptResource)
				.render(Map.of("metadataSection", metadataSection, "pdfText", pdfText));

		try {
			String responseText = callLlm(promptText, MAX_TOKENS_ARTIKULATIONSSCHEMA);

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
	public String generateDeckblatt(String pdfText, Map<String, Object> metadata) {
		String metadataSection = buildMetadataSection(metadata);
		String promptText = new PromptTemplate(deckblattPromptResource)
				.render(Map.of("metadataSection", metadataSection, "pdfText", pdfText));

		try {
			String responseText = callLlm(promptText, MAX_TOKENS_DECKBLATT);

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
	public String generateHintergrundwissen(String pdfText, Map<String, Object> metadata) {
		String metadataSection = buildMetadataSection(metadata);
		String promptText = new PromptTemplate(hintergrundwissenPromptResource)
				.render(Map.of("metadataSection", metadataSection, "pdfText", pdfText));

		try {
			String responseText = callLlm(promptText, MAX_TOKENS_HINTERGRUNDWISSEN);

			logger.debug("LLM Hintergrundwissen Response: {}", responseText);

			return extractMarkdownPayload(responseText);
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate Hintergrundwissen: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate an age-appropriate exercise sheet and its matching solution in a
	 * single LLM call, guaranteeing the two documents correspond exactly.
	 *
	 * @param pdfText
	 *            extracted text from the PDF
	 * @param metadata
	 *            user-adjusted activity metadata (ageMin/ageMax drive difficulty)
	 * @return map with keys "uebung" and "uebung_loesung", each containing markdown
	 */
	public Map<String, String> generateUebungAndLoesung(String pdfText, Map<String, Object> metadata) {
		return generateUebungAndLoesung(pdfText, metadata, null);
	}

	public Map<String, String> generateUebungAndLoesung(String pdfText, Map<String, Object> metadata,
			List<byte[]> pdfPageImages) {
		String metadataSection = buildMetadataSection(metadata);

		// When images are provided, omit the extracted PDF text from the prompt — the
		// images already contain the teaching material. Sending both wastes context
		// tokens and can overflow the model's context window, truncating the output.
		boolean hasImages = pdfPageImages != null && !pdfPageImages.isEmpty();
		String textForPrompt = hasImages ? "" : pdfText;

		String promptText = new PromptTemplate(uebungPromptResource)
				.render(Map.of("metadataSection", metadataSection, "pdfText", textForPrompt));

		boolean useVisual = isVisionEnabled();
		int maxTokens = useVisual ? visualMaxTokens : MAX_TOKENS_UEBUNG;

		try {
			String responseText = useVisual
					? callVisualLlm(promptText, pdfPageImages, maxTokens)
					: callLlm(promptText, maxTokens);

			logger.debug("LLM Uebung Response: {}", responseText);

			Map<String, String> generatedMarkdowns = extractUebungPayload(responseText);
			String imageContext = buildExerciseImageContext(generatedMarkdowns);
			return replaceExerciseImagePlaceholders(generatedMarkdowns, imageContext);
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate Übung and Lösung: " + e.getMessage(), e);
		}
	}

	public boolean isVisionEnabled() {
		return visualModelName != null && !visualModelName.isBlank();
	}

	/** Standard LLM call — text only, uses the default model. */
	private String callLlm(String promptText, int maxTokens) {
		OpenAiChatOptions options = OpenAiChatOptions.builder().maxTokens(maxTokens).build();
		return chatClient.prompt().user(promptText).options(options).call().content();
	}

	/**
	 * Visual LLM call — overrides the model and optionally attaches PDF page
	 * images.
	 */
	private String callVisualLlm(String promptText, List<byte[]> pdfPageImages, int maxTokens) {
		OpenAiChatOptions options = OpenAiChatOptions.builder().model(visualModelName).maxTokens(maxTokens).build();
		if (pdfPageImages != null && !pdfPageImages.isEmpty()) {
			logger.info("Sending {} PDF page images to visual model '{}'", pdfPageImages.size(), visualModelName);
			Media[] mediaArray = new Media[pdfPageImages.size()];
			for (int i = 0; i < pdfPageImages.size(); i++) {
				mediaArray[i] = new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(pdfPageImages.get(i)));
			}
			return chatClient.prompt().user(u -> u.text(promptText).media(mediaArray)).options(options).call()
					.content();
		}
		return chatClient.prompt().user(promptText).options(options).call().content();
	}

	// Accept both styles: with ===_END=== delimiters or without (content
	// delimited by the next ===_START=== marker or end of string).
	private static final Pattern UEBUNG_PATTERN = Pattern
			.compile("===UEBUNG_START===\\s*(.*?)\\s*(?:===UEBUNG_END===|===LOESUNG_START===)", Pattern.DOTALL);
	private static final Pattern LOESUNG_PATTERN = Pattern
			.compile("===LOESUNG_START===\\s*(.*?)\\s*(?:===LOESUNG_END===\\s*)?$", Pattern.DOTALL);

	private Map<String, String> extractUebungPayload(String rawResponse) {
		if (rawResponse == null || rawResponse.trim().isEmpty()) {
			throw new IllegalStateException("LLM returned an empty response");
		}

		Matcher uebungMatcher = UEBUNG_PATTERN.matcher(rawResponse);
		Matcher loesungMatcher = LOESUNG_PATTERN.matcher(rawResponse);

		if (!uebungMatcher.find() || !loesungMatcher.find()) {
			throw new IllegalStateException(
					"LLM response missing required delimiters ===UEBUNG_START=== / ===LOESUNG_START===");
		}

		Map<String, String> result = new LinkedHashMap<>();
		result.put("uebung", uebungMatcher.group(1).trim());
		result.put("uebung_loesung", loesungMatcher.group(1).trim());
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
				replacedMarkdowns.put(key,
						replaceImagePlaceholders(markdowns.get(key), imageMarkdownCache, imageDescriptionCache, pdfText));
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
		String exerciseMarkdown = generatedMarkdowns.getOrDefault("uebung", "").trim();
		String solutionMarkdown = generatedMarkdowns.getOrDefault("uebung_loesung", "").trim();
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
		return replaceImagePlaceholders(markdown, imageMarkdownCache, new HashMap<>(), pdfText);
	}

	String replaceImagePlaceholders(String markdown, Map<String, String> imageMarkdownCache,
			Map<String, String> imageDescriptionCache, String pdfText) {
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
				logger.info(
						"Reusing exercise image id '{}' with a different description. Keeping the first generated image.",
						placeholder.id());
			}
			String imageMarkdown = imageMarkdownCache.computeIfAbsent(placeholder.cacheKey(),
					key -> generateImageMarkdownSafely(placeholder, pdfText));
			matcher.appendReplacement(replaced, Matcher.quoteReplacement(imageMarkdown));
		}
		matcher.appendTail(replaced);
		return replaced.toString();
	}

	public String generateImageMarkdown(String id, String description, String contextText) {
		if (exerciseImageModel == null) {
			throw new IllegalStateException("Exercise image model is not configured");
		}

		ImageResponse response = exerciseImageModel.call(new ImagePrompt(buildExerciseImagePrompt(description, contextText)));
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
		return EMBEDDED_IMAGE_PATTERN.matcher(markdown).replaceAll("[Bild]");
	}

	String buildExerciseImagePrompt(String description, String contextText) {
		String normalizedDescription = description == null ? "" : description.trim();
		String normalizedContextText = stripEmbeddedImages(contextText == null ? "" : contextText.trim());
		if (exerciseImagePromptResource == null) {
			return DEFAULT_EXERCISE_IMAGE_PROMPT_TEMPLATE
					.replace("{contextText}", normalizedContextText)
					.replace("{description}", normalizedDescription);
		}
		return new PromptTemplate(exerciseImagePromptResource)
				.render(Map.of("description", normalizedDescription, "contextText", normalizedContextText));
	}

	private String toMarkdownImageFromUrl(String imageUrl, String altText) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
					.GET()
					.timeout(IMAGE_FETCH_TIMEOUT)
					.build();
			HttpResponse<byte[]> response = IMAGE_FETCH_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IllegalStateException("Failed to download generated image: HTTP " + response.statusCode());
			}

			String mimeType = response.headers().firstValue("Content-Type")
					.map(value -> value.split(";", 2)[0].trim())
					.filter(StringUtils::hasText)
					.orElseGet(() -> inferImageMimeType(imageUrl));
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
			return new ImagePlaceholder("desc:" + normalized, null, normalized, buildImagePlaceholderMarker(null, normalized));
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
