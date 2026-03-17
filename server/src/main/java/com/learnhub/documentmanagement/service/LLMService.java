package com.learnhub.documentmanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class LLMService {

	private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
	private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```",
			Pattern.DOTALL);

	@Value("classpath:prompts/ActivityDataExtraction.st")
	private Resource extractionPromptResource;

	@Value("classpath:prompts/ArtikulationsschemaGeneration.st")
	private Resource artikulationsschemaPromptResource;

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public LLMService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
		ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
		this.chatClient = builder != null ? builder.build() : null;
	}

	public Map<String, Object> extractActivityData(String pdfText) {
		if (chatClient == null) {
			throw new IllegalStateException("ChatClient is not available. Please configure a ChatModel.");
		}

		String promptText = new PromptTemplate(extractionPromptResource).render(Map.of("pdfText", pdfText));

		try {
			String responseText = chatClient.prompt().user(promptText).call().content();

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
		if (chatClient == null) {
			throw new IllegalStateException("ChatClient is not available. Please configure a ChatModel.");
		}

		String metadataSection = buildMetadataSection(metadata);
		String promptText = new PromptTemplate(artikulationsschemaPromptResource)
				.render(Map.of("metadataSection", metadataSection, "pdfText", pdfText));

		try {
			String responseText = chatClient.prompt().user(promptText).call().content();

			logger.debug("LLM Artikulationsschema Response: {}", responseText);

			return extractMarkdownPayload(responseText);
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate Artikulationsschema: " + e.getMessage(), e);
		}
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
		metadataSection.append("\nVerwende diese Metadaten für Klassenstufe, Dauer, Thema und die Spalte Medien/Material.\n");
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
