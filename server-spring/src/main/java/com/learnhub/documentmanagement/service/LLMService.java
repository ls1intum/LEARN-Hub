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
}
