package com.learnhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		// DB_CLOSE_DELAY=-1 keeps the in-memory schema alive across connections for
		// the whole JVM; without it H2 drops the DB when the first connection closes.
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		// The JPA entities use Postgres-only column types (jsonb, custom enums like
		// `userrole`/`energylevel`), which H2 cannot generate. This smoke test only
		// verifies that the Spring context/bean graph wires up, not the schema, so
		// skip DDL generation entirely (Flyway is disabled too). This keeps the test
		// deterministic across environments (it otherwise passed locally only because
		// the H2 schema-export errors happened to be non-fatal).
		"spring.jpa.hibernate.ddl-auto=none", "spring.flyway.enabled=false",
		// Never run the admin seeder during the context-load smoke test.
		"app.db-seed.enabled=false",
		// Spring AI's OpenAI auto-configuration refuses to instantiate its client
		// beans without an API key, and LLMService requires a non-null ChatClient -
		// so the context can't load on CI (no keys in the environment). Provide a
		// dummy key: the beans are built but never called during a context-load smoke
		// test (no network happens at construction). This top-level key also backs the
		// OpenAI image client. The image-specific api-key is left empty on purpose so
		// the exercise image model stays disabled during the smoke test.
		"spring.ai.openai.api-key=test-key" })
class LearnHubApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the Spring application context loads successfully
	}

}
