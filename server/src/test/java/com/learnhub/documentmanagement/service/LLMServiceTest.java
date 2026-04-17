package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

class LLMServiceTest {

	@Test
	void replaceExerciseImagePlaceholdersReusesGeneratedImageAcrossDocuments() {
		CountingImageModel imageModel = new CountingImageModel("ZmFrZS1pbWFnZQ==");
		LLMService service = new LLMService(unsupportedChatClient(), imageModel);

		Map<String, String> replaced = service.replaceExerciseImagePlaceholders(Map.of(
				"uebung", """
						# Uebung

						[[IMAGE_PLACEHOLDER: bunte Pixelgrafik mit drei Quadraten]]
						""",
				"uebung_loesung", """
						# Loesung

						[[IMAGE_PLACEHOLDER: bunte Pixelgrafik mit drei Quadraten]]
						"""), "PDF Kontext");

		assertThat(imageModel.calls).isEqualTo(1);
		assertThat(replaced.get("uebung")).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==")
				.doesNotContain("IMAGE_PLACEHOLDER");
		assertThat(replaced.get("uebung_loesung")).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==")
				.doesNotContain("IMAGE_PLACEHOLDER");
	}

	@Test
	void replaceExerciseImagePlaceholdersReusesImageAcrossDocumentsById() {
		CountingImageModel imageModel = new CountingImageModel("ZmFrZS1pbWFnZQ==");
		LLMService service = new LLMService(unsupportedChatClient(), imageModel);

		Map<String, String> replaced = service.replaceExerciseImagePlaceholders(Map.of(
				"uebung", """
						[[IMAGE_PLACEHOLDER:id=labyrinth-1: Ein 4x4-Labyrinth mit Start unten links und Ziel oben rechts.]]
						""",
				"uebung_loesung", """
						[[IMAGE_PLACEHOLDER:id=labyrinth-1: Das gleiche Labyrinth aus der Aufgabe mit anderem Beschreibungstext.]]
						"""), "PDF Kontext");

		assertThat(imageModel.calls).isEqualTo(1);
		assertThat(replaced.get("uebung")).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==");
		assertThat(replaced.get("uebung_loesung")).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==");
	}

	@Test
	void replaceImagePlaceholdersHandlesBrokenClosingBrackets() {
		CountingImageModel imageModel = new CountingImageModel("ZmFrZS1pbWFnZQ==");
		LLMService service = new LLMService(unsupportedChatClient(), imageModel);

		String replaced = service.replaceImagePlaceholders("""
				[[IMAGE_PLACEHOLDER:id=igel-roboter-ziel: Informatik-Igel und Roboter stehen nebeneinander auf dem Zielfeld des Labyrinths, beide laecheln, bunte Konfetti im Hintergrund, froehlicher Stil, Vordergrund zeigt das Zielfeld, Hintergrund das Labyrinth.]
				]
				""", new java.util.HashMap<>(), "Kontext");

		assertThat(imageModel.calls).isEqualTo(1);
		assertThat(replaced).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==")
				.doesNotContain("IMAGE_PLACEHOLDER");
	}

	@Test
	void generateImageMarkdownWrapsDescriptionWithSharedExercisePrompt() {
		CountingImageModel imageModel = new CountingImageModel("ZmFrZS1pbWFnZQ==");
		LLMService service = new LLMService(unsupportedChatClient(), imageModel);

		service.generateImageMarkdown("Ein 4x4-Labyrinth mit Start unten links.", "Im Material geht es um Wege im Raster.");

		assertThat(imageModel.lastPromptText).contains("school exercise sheets")
				.contains("must be accurate enough")
				.contains("Ein 4x4-Labyrinth mit Start unten links.")
				.contains("Im Material geht es um Wege im Raster.");
	}

	@Test
	void buildExerciseImageContextUsesGeneratedExerciseAndSolutionText() {
		LLMService service = new LLMService(unsupportedChatClient());

		String context = service.buildExerciseImageContext(Map.of(
				"uebung", "# Übungsblatt\n\nAufgabe 1 mit Bild.",
				"uebung_loesung", "# Lösungsblatt\n\nLösung zu Aufgabe 1."));

		assertThat(context).contains("Generiertes Übungsblatt:")
				.contains("Aufgabe 1 mit Bild.")
				.contains("Generiertes Lösungsblatt:")
				.contains("Lösung zu Aufgabe 1.");
	}

	@Test
	void replaceExerciseImagePlaceholdersLeavesMarkerWhenNoImageModelExists() {
		LLMService service = new LLMService(unsupportedChatClient());
		String markdown = "[[IMAGE_PLACEHOLDER: einfacher Testprompt]]";

		String replaced = service.replaceImagePlaceholders(markdown, new java.util.HashMap<>(), "PDF Kontext");

		assertThat(replaced).isEqualTo(markdown);
	}

	private static ChatClient unsupportedChatClient() {
		return (ChatClient) Proxy.newProxyInstance(ChatClient.class.getClassLoader(), new Class[]{ChatClient.class},
				(proxy, method, args) -> {
					throw new UnsupportedOperationException("ChatClient should not be called in this test");
				});
	}

	private static final class CountingImageModel implements ImageModel {

		private final String base64;
		private int calls;
		private String lastPromptText;

		private CountingImageModel(String base64) {
			this.base64 = base64;
		}

		@Override
		public ImageResponse call(ImagePrompt request) {
			calls++;
			lastPromptText = request.getInstructions().get(0).getText();
			return new ImageResponse(java.util.List.of(new ImageGeneration(new Image(null, base64))));
		}
	}
}
