package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.core.io.ClassPathResource;

class LLMServiceTest {

	@Test
	void replaceExerciseImagePlaceholdersReusesGeneratedImageAcrossDocuments() {
		CountingImageModel imageModel = new CountingImageModel("ZmFrZS1pbWFnZQ==");
		LLMService service = testService(imageModel);

		Map<String, String> replaced = service.replaceExerciseImagePlaceholders(Map.of("exercise", """
				# Uebung

				[[IMAGE_PLACEHOLDER: bunte Pixelgrafik mit drei Quadraten]]
				""", "exercise_solution", """
				# Loesung

				[[IMAGE_PLACEHOLDER: bunte Pixelgrafik mit drei Quadraten]]
				"""), "PDF Kontext");

		assertThat(imageModel.calls).isEqualTo(1);
		assertThat(replaced.get("exercise")).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==")
				.doesNotContain("IMAGE_PLACEHOLDER");
		assertThat(replaced.get("exercise_solution")).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==")
				.doesNotContain("IMAGE_PLACEHOLDER");
	}

	@Test
	void replaceExerciseImagePlaceholdersReusesImageAcrossDocumentsById() {
		CountingImageModel imageModel = new CountingImageModel("ZmFrZS1pbWFnZQ==");
		LLMService service = testService(imageModel);

		Map<String, String> replaced = service.replaceExerciseImagePlaceholders(Map.of("exercise", """
				[[IMAGE_PLACEHOLDER:id=labyrinth-1: Ein 4x4-Labyrinth mit Start unten links und Ziel oben rechts.]]
				""", "exercise_solution",
				"""
						[[IMAGE_PLACEHOLDER:id=labyrinth-1: Das gleiche Labyrinth aus der Aufgabe mit anderem Beschreibungstext.]]
						"""),
				"PDF Kontext");

		assertThat(imageModel.calls).isEqualTo(1);
		assertThat(replaced.get("exercise")).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==");
		assertThat(replaced.get("exercise_solution")).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==");
	}

	@Test
	void replaceImagePlaceholdersHandlesBrokenClosingBrackets() {
		CountingImageModel imageModel = new CountingImageModel("ZmFrZS1pbWFnZQ==");
		LLMService service = testService(imageModel);

		String replaced = service.replaceImagePlaceholders(
				"""
						[[IMAGE_PLACEHOLDER:id=igel-roboter-ziel: Informatik-Igel und Roboter stehen nebeneinander auf dem Zielfeld des Labyrinths, beide laecheln, bunte Konfetti im Hintergrund, froehlicher Stil, Vordergrund zeigt das Zielfeld, Hintergrund das Labyrinth.]
						]
						""",
				new java.util.HashMap<>(), "Kontext");

		assertThat(imageModel.calls).isEqualTo(1);
		assertThat(replaced).contains("data:image/png;base64,ZmFrZS1pbWFnZQ==").doesNotContain("IMAGE_PLACEHOLDER");
	}

	@Test
	void generateImageMarkdownWrapsDescriptionWithSharedExercisePrompt() {
		CountingImageModel imageModel = new CountingImageModel("ZmFrZS1pbWFnZQ==");
		LLMService service = testService(imageModel);

		service.generateImageMarkdown("labyrinth-1", "Ein 4x4-Labyrinth mit Start unten links.",
				"Im Material geht es um Wege im Raster.");

		assertThat(imageModel.lastPromptText).contains("school exercise sheets").contains("must be accurate enough")
				.contains("Ein 4x4-Labyrinth mit Start unten links.")
				.contains("Im Material geht es um Wege im Raster.");
	}

	@Test
	void buildExerciseImagePromptStripsEmbeddedBase64ImageDataFromContext() {
		LLMService service = testService();
		String base64 = "A".repeat(50_000);

		String prompt = service.buildExerciseImagePrompt("Ein Labyrinth.", """
				Aufgabe
				<!-- learnhub-image:id=labyrinth-1; prompt=Ein Labyrinth. -->
				![labyrinth-1](data:image/png;base64,%s)
				Loesung
				""".formatted(base64));

		assertThat(prompt).contains("Aufgabe").contains("[Bild]").contains("Loesung").doesNotContain(base64);
		assertThat(prompt.length()).isLessThan(32_000);
	}

	@Test
	void buildExerciseImagePromptStripsBareBase64DataUris() {
		LLMService service = testService();
		String base64 = "A".repeat(50_000);

		String prompt = service.buildExerciseImagePrompt("Ein Labyrinth.",
				"Vorher data:image/png;base64," + base64 + " Nachher");

		assertThat(prompt).contains("Vorher [Bilddaten entfernt] Nachher").doesNotContain(base64);
		assertThat(prompt.length()).isLessThan(32_000);
	}

	@Test
	void buildExerciseImagePromptStripsHtmlBase64Images() {
		LLMService service = testService();
		String base64 = "A".repeat(50_000);

		String prompt = service.buildExerciseImagePrompt("Ein Labyrinth.",
				"Vorher <img alt=\"Labyrinth\" src=\"data:image/png;base64," + base64 + "\" /> Nachher");

		assertThat(prompt).contains("Vorher [Bild] Nachher").doesNotContain(base64);
		assertThat(prompt.length()).isLessThan(32_000);
	}

	@Test
	void buildExerciseImagePromptTruncatesOversizedText() {
		LLMService service = testService();

		String prompt = service.buildExerciseImagePrompt("D".repeat(20_000), "C".repeat(80_000));

		assertThat(prompt).contains("omitted");
		assertThat(prompt.length()).isLessThan(32_000);
	}

	@Test
	void buildExerciseImageContextUsesGeneratedExerciseAndSolutionText() {
		LLMService service = testService();

		String context = service.buildExerciseImageContext(Map.of("exercise", "# Übungsblatt\n\nAufgabe 1 mit Bild.",
				"exercise_solution", "# Lösungsblatt\n\nLösung zu Aufgabe 1."));

		assertThat(context).contains("Generiertes Übungsblatt:").contains("Aufgabe 1 mit Bild.")
				.contains("Generiertes Lösungsblatt:").contains("Lösung zu Aufgabe 1.");
	}

	@Test
	void replaceExerciseImagePlaceholdersLeavesMarkerWhenNoImageModelExists() {
		LLMService service = testService();
		String markdown = "[[IMAGE_PLACEHOLDER: einfacher Testprompt]]";

		String replaced = service.replaceImagePlaceholders(markdown, new java.util.HashMap<>(), "PDF Kontext");

		assertThat(replaced).isEqualTo(markdown);
	}

	private static LLMService testService() {
		return testService(null);
	}

	private static LLMService testService(ImageModel imageModel) {
		LLMService service = new LLMService(unsupportedChatClient(), imageModel);
		setField(service, "exerciseImagePromptResource", new ClassPathResource("prompts/ExerciseImageGeneration.st"));
		return service;
	}

	private static void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Failed to set test field: " + fieldName, e);
		}
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
