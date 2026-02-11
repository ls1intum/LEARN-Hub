package com.learnhub.documentmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.config.OllamaConfig;

import org.springframework.ai.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaConfig.class);

    @Autowired
    private OllamaChatModel ollamaChatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> extractActivityData(String pdfText) {
        String promptText = buildExtractionPrompt(pdfText);

        try {
            Prompt prompt = new Prompt(promptText);
            ChatResponse response = ollamaChatModel.call(prompt);
            String responseText = response.getResult().getOutput().getContent();

            logger.info("LLM Response: {}", responseText);
            logger.info("LLM Metadata: {}", response.getMetadata());

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
