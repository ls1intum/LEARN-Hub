package com.learnhub.documentmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    public LLMService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Map<String, Object> extractActivityData(String pdfText) {
        if (chatClient == null) {
            throw new IllegalStateException("ChatClient is not available. Please configure a ChatModel.");
        }

        String promptText = buildExtractionPrompt(pdfText);

        try {
            String responseText = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            logger.debug("LLM Response: {}", responseText);

            // Parse JSON response
            return objectMapper.readValue(responseText, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract activity data from PDF: " + e.getMessage(), e);
        }
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
