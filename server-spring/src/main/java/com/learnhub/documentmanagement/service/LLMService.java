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
import org.springframework.stereotype.Service;

@Service
public class LLMService {

	private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
	private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```",
			Pattern.DOTALL);

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

		String promptText = buildExtractionPrompt(pdfText);

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
	 */
	public String generateArtikulationsschema(String pdfText) {
		if (chatClient == null) {
			throw new IllegalStateException("ChatClient is not available. Please configure a ChatModel.");
		}

		String promptText = buildArtikulationsschemaPrompt(pdfText);

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

	private String buildExtractionPrompt(String pdfText) {
		return String.format(
				"""
						Extract the educational activity from this text and return JSON only.

						Required JSON structure:
						{
						  "data": {
						    "name": "activity name",
						    "description": "brief description",
						    "age_min": 6-15,
						    "age_max": 6-15,
						    "format": "unplugged|digital|hybrid",
						    "bloom_level": "remember|understand|apply|analyze|evaluate|create",
						    "duration_min_minutes": 5-300,
						    "duration_max_minutes": optional number,
						    "resources_needed": optional array from ["computers", "tablets", "handouts", "blocks", "electronics", "stationery"],
						    "topics": optional array from ["decomposition", "patterns", "abstraction", "algorithms"],
						    "mental_load": optional "low|medium|high",
						    "physical_energy": optional "low|medium|high",
						    "prep_time_minutes": optional number,
						    "cleanup_time_minutes": optional number,
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

	private String buildArtikulationsschemaPrompt(String pdfText) {
		return String.format(
				"""
						You are a pedagogical expert. Analyze the following teaching material and produce an Artikulationsschema (lesson articulation schema).

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
				pdfText);
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
