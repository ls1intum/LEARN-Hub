package com.learnhub.documentmanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Service
public class LLMService {

	private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
	private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```",
			Pattern.DOTALL);

	@Value("${llm.visual.model:}")
	private String visualModelName;

	@Value("${llm.visual.max-tokens:8000}")
	private int visualMaxTokens;

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

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public LLMService(ChatClient chatClient) {
		if (chatClient == null) {
			throw new IllegalStateException("ChatClient is not available. Please configure a ChatModel.");
		}
		this.chatClient = chatClient;
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

			return extractUebungPayload(responseText);
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

		Map<String, String> result = new HashMap<>();
		result.put("uebung", uebungMatcher.group(1).trim());
		result.put("uebung_loesung", loesungMatcher.group(1).trim());
		return result;
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
