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

		String promptText = buildArtikulationsschemaPrompt(pdfText, metadata);

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
				metadataSection.append("- Benötigte Materialien: ").append(metadata.get("resourcesNeeded"))
						.append("\n");
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
			metadataSection.append(
					"\nVerwende diese Metadaten für Klassenstufe, Dauer, Thema und die Spalte Medien/Material.\n");
		}

		return String.format(
				"""
						Du bist ein Experte für Pädagogik und Unterrichtsplanung. Analysiere das folgende Unterrichtsmaterial und erstelle ein Artikulationsschema nach dem AVIVA+-Modell.
						%s
						WICHTIGE REGELN:
						1. Falls der Text bereits ein Artikulationsschema oder eine Phasenstruktur enthält, nutze all diese Daten in deinem AVIVA+-Schema und erkläre alle vorhandenen Schritte in den Handlungsanweisungen.
						2. Falls kein Schema vorhanden ist, erstelle ein konservatives, klar strukturiertes Schema auf Grundlage des Materials.
						3. Verwende das AVIVA+-Phasenmodell mit den folgenden Phasen (ALLE 6 PHASEN MÜSSEN ALS MIN. EINE SPALTE VORHANDEN SEIN, AUCH WENN SIE NUR KURZ ANGEDEUTET WERDEN):
						   (+) Lernatmosphäre schaffen - Vertrauensvolle Umgebung und positive Grundstimmung herstellen.
						   (A) Ankommen und Ausrichten - Relevanz motivieren, Lernziele und Ablauf bekanntgeben.
						   (V) Vorwissen aktivieren - Vorwissen identifizieren und reaktivieren, damit sich Neues mit Bekanntem verbinden kann.
						   (I) Informieren - Neue Inhalte vorstellen, die als Grundlage für den Kompetenzaufbau dienen.
						   (V) Verarbeiten - Gelerntes anwenden, vertiefen und üben, um es zu verfestigen.
						   (A) Auswerten - Lernerfolg überprüfen und den Lehr-Lernprozess reflektieren.
						4. Sei detailiert und konkret in der Beschreibung der Handlungsschritte!

						AUSGABEFORMAT:
						Gib NUR ein Markdown-Dokument mit exakt folgender Struktur zurück:

						# Artikulationsschema

						**Thema:** [Aus dem Material abgeleitetes Thema]
						**Klassenstufe:** [Klassenstufe/Alter falls erwähnt, sonst "k.A."]
						**Dauer:** [Gesamtdauer in Minuten falls erwähnt, sonst schätzen]

						| Zeit | Phase | Handlungsschritte | Sozialform | Kompetenzen | Medien/Material |
						|------|-------|-------------------|------------|-------------|-----------------|
						| ... | (+) Lernatmosphäre schaffen | ... | ... | ... | ... |
						| ... | (A) Ankommen / Ausrichten | ... | ... | ... | ... |
						| ... | (V) Vorwissen aktivieren | ... | ... | ... | ... |
						| ... | (I) Informieren | ... | ... | ... | ... |
						| ... | (V) Verarbeiten | ... | ... | ... | ... |
						| ... | (A) Auswerten | ... | ... | ... | ... |

						SPALTEN-RICHTLINIEN:
						- Zeit: Dauer jeder Phase (z.B. "5 min", "15 min")
						- Phase: Eine der AVIVA+-Phasen (siehe oben)
						- Handlungsschritte: Konkrete Lehrer- und Schüleraktivitäten
						- Sozialform: z.B. Plenum, Einzelarbeit, Partnerarbeit, Gruppenarbeit
						- Kompetenzen: Angestrebte Lernziele bzw. Kompetenzen
						- Medien/Material: Beschreibe die benötigten Materialien konkret (z.B. "Arbeitsblatt zu Algorithmen", "Stifte und Papier", "Laptop mit Internetzugang"). Verweise NICHT auf Seitenzahlen oder Dokumentabschnitte, sondern beschreibe das Material so, dass es eigenständig verständlich ist.

						Gib NUR das Markdown zurück. Keine Erklärungen, keine Code-Block-Umschließungen.

						Unterrichtsmaterial:
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
