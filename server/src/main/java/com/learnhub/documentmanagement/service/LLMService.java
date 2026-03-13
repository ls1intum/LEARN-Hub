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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

@Service
public class LLMService {

	private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
	private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```",
			Pattern.DOTALL);
	private static final MimeType APPLICATION_PDF = MimeType.valueOf("application/pdf");

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public LLMService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
		ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
		this.chatClient = builder != null ? builder.build() : null;
	}

	public Map<String, Object> extractActivityData(String pdfText, byte[] pdfContent) {
		if (chatClient == null) {
			throw new IllegalStateException("ChatClient is not available. Please configure a ChatModel.");
		}

		String promptText = buildExtractionPrompt(pdfText);

		try {
			String responseText = chatClient.prompt().user(u -> u.text(promptText)
					.media(APPLICATION_PDF, new ByteArrayResource(pdfContent))).call().content();

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
	 * @param pdfText    extracted text from the PDF
	 * @param pdfContent raw PDF bytes attached as media
	 * @param metadata   user-adjusted activity metadata to inform the schema
	 */
	public String generateArtikulationsschema(String pdfText, byte[] pdfContent, Map<String, Object> metadata) {
		if (chatClient == null) {
			throw new IllegalStateException("ChatClient is not available. Please configure a ChatModel.");
		}

		String promptText = buildArtikulationsschemaPrompt(pdfText, metadata);

		try {
			String responseText = chatClient.prompt().user(u -> u.text(promptText)
					.media(APPLICATION_PDF, new ByteArrayResource(pdfContent))).call().content();

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

	private String buildExtractionPrompt(String pdfText) {
		return String.format(
				"""
						Extract the educational activity from this text and return JSON only.

						Required JSON structure:
						{
						  "data": {
						    "name": "activity name",
						    "description": "brief description",
						    "ageMin": 6-15,
						    "ageMax": 6-15,
						    "format": "unplugged|digital|hybrid",
						    "bloomLevel": "remember|understand|apply|analyze|evaluate|create",
						    "durationMinMinutes": 5-300,
						    "durationMaxMinutes": optional number,
						    "resourcesNeeded": optional array from ["computers", "tablets", "handouts", "blocks", "electronics", "stationery"],
						    "topics": optional array from ["decomposition", "patterns", "abstraction", "algorithms"],
						    "mentalLoad": optional "low|medium|high",
						    "physicalEnergy": optional "low|medium|high",
						    "prepTimeMinutes": optional number,
						    "cleanupTimeMinutes": optional number,
						    "source": optional string
						  },
						  "confidence": 0.0-1.0
						}

						Notes:
						- For optional arrays, use [] if information is not clear
						- Choose closest matching value from allowed options
						- Output only the JSON object, no explanation

						Text:
						%s
						""",
				pdfText);
	}

	private String buildArtikulationsschemaPrompt(String pdfText, Map<String, Object> metadata) {
		StringBuilder metadataSection = new StringBuilder();
		if (metadata != null && !metadata.isEmpty()) {
			metadataSection.append("\n\nACTIVITY METADATA (confirmed by the teacher):\n");
			if (metadata.containsKey("name")) {
				metadataSection.append("- Name: ").append(metadata.get("name")).append("\n");
			}
			if (metadata.containsKey("description")) {
				metadataSection.append("- Description: ").append(metadata.get("description")).append("\n");
			}
			if (metadata.containsKey("ageMin") || metadata.containsKey("ageMax")) {
				metadataSection.append("- Age range: ").append(metadata.getOrDefault("ageMin", "?")).append("-")
						.append(metadata.getOrDefault("ageMax", "?")).append("\n");
			}
			if (metadata.containsKey("format")) {
				metadataSection.append("- Format: ").append(metadata.get("format")).append("\n");
			}
			if (metadata.containsKey("bloomLevel")) {
				metadataSection.append("- Bloom level: ").append(metadata.get("bloomLevel")).append("\n");
			}
			if (metadata.containsKey("durationMinMinutes")) {
				metadataSection.append("- Duration (min): ").append(metadata.get("durationMinMinutes"));
				if (metadata.containsKey("durationMaxMinutes")) {
					metadataSection.append("-").append(metadata.get("durationMaxMinutes"));
				}
				metadataSection.append(" minutes\n");
			}
			if (metadata.containsKey("resourcesNeeded")) {
				metadataSection.append("- Resources needed: ").append(metadata.get("resourcesNeeded")).append("\n");
			}
			if (metadata.containsKey("topics")) {
				metadataSection.append("- Topics: ").append(metadata.get("topics")).append("\n");
			}
			if (metadata.containsKey("mentalLoad")) {
				metadataSection.append("- Mental load: ").append(metadata.get("mentalLoad")).append("\n");
			}
			if (metadata.containsKey("physicalEnergy")) {
				metadataSection.append("- Physical energy: ").append(metadata.get("physicalEnergy")).append("\n");
			}
			if (metadata.containsKey("source")) {
				metadataSection.append("- Source: ").append(metadata.get("source")).append("\n");
			}
			metadataSection.append(
					"\nUse this metadata to inform the Klassenstufe, Dauer, Thema, and material/media columns.\n");
		}

		return String.format(
				"""
						You are a pedagogical expert. Analyze the following teaching material and produce an Artikulationsschema (lesson articulation schema).
						%s
						IMPORTANT RULES:
						1. If the text already contains an Artikulationsschema or lesson phase structure, extract and normalize it faithfully.
						2. If no schema exists, generate a conservative, clearly structured one grounded in the material.
						3. Do NOT invent content that is not supported by the source material.
						4. Use the standard instructional phase flow: Einstieg, Erarbeitung, Ergebnissicherung. Add Reflexion/Transfer only if supported by the material.

						OUTPUT FORMAT:
						Return ONLY a markdown document with this exact structure:

						# Artikulationsschema

						**Thema:** [Topic derived from the material]
						**Klassenstufe:** [Grade/level if mentioned, otherwise "k.A."]
						**Dauer:** [Total duration in minutes if mentioned, otherwise estimate]

						| Zeit | Phase | Handlungsschritte | Sozialform | Kompetenzen | Medien/Material |
						|------|-------|-------------------|------------|-------------|-----------------|
						| ... | Einstieg | ... | ... | ... | ... |
						| ... | Erarbeitung | ... | ... | ... | ... |
						| ... | Ergebnissicherung | ... | ... | ... | ... |

						COLUMN GUIDELINES:
						- Zeit: Duration for each phase (e.g. "5 min", "15 min")
						- Phase: One of Einstieg, Erarbeitung, Ergebnissicherung, Reflexion, Transfer
						- Handlungsschritte: Concrete teacher and student actions
						- Sozialform: e.g. Plenum, Einzelarbeit, Partnerarbeit, Gruppenarbeit
						- Kompetenzen: Learning objectives or competencies addressed
						- Medien/Material: Required materials and media

						Return ONLY the markdown. No explanations, no code blocks wrapping the markdown.

						Teaching material:
						%s
						""",
				metadataSection.toString(), pdfText);
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
