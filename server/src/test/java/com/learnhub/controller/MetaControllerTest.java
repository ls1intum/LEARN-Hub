package com.learnhub.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnhub.dto.response.EnvironmentResponse;
import com.learnhub.dto.response.FieldValuesResponse;
import com.learnhub.dto.response.HelloResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

class MetaControllerTest {

	private MetaController metaController;

	@BeforeEach
	void setUp() {
		metaController = new MetaController();
		ReflectionTestUtils.setField(metaController, "environment", "local");
	}

	@Test
	void helloReturnsGreeting() {
		ResponseEntity<HelloResponse> response = metaController.hello();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getMessage()).isEqualTo("Hello, world!");
	}

	@Test
	void getFieldValuesReturnsAllEnumOptions() {
		ResponseEntity<FieldValuesResponse> response = metaController.getFieldValues();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		FieldValuesResponse body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getFormat()).containsExactly("unplugged", "digital", "hybrid");
		assertThat(body.getResourcesAvailable()).contains("computers", "tablets", "handouts");
		assertThat(body.getBloomLevel()).containsExactly("remember", "understand", "apply", "analyze", "evaluate",
				"create");
		assertThat(body.getTopics()).containsExactly("decomposition", "patterns", "abstraction", "algorithms");
		assertThat(body.getMentalLoad()).containsExactly("low", "medium", "high");
		assertThat(body.getPhysicalEnergy()).containsExactly("low", "medium", "high");
		assertThat(body.getPriorityCategories()).contains("age_appropriateness", "bloom_level_match");
	}

	@Test
	void getEnvironmentReturnsConfiguredEnvironment() {
		ReflectionTestUtils.setField(metaController, "environment", "production");

		ResponseEntity<EnvironmentResponse> response = metaController.getEnvironment();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getEnvironment()).isEqualTo("production");
	}

	@Test
	void getEnvironmentDefaultsToLocal() {
		ResponseEntity<EnvironmentResponse> response = metaController.getEnvironment();

		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getEnvironment()).isEqualTo("local");
	}
}
