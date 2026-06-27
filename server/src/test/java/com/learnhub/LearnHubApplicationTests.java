package com.learnhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		// DB_CLOSE_DELAY=-1 keeps the in-memory schema alive across connections for
		// the whole JVM; without it H2 drops the DB when the first connection closes.
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		// Never run the admin seeder during the context-load smoke test.
		"app.db-seed.enabled=false"})
class LearnHubApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the Spring application context loads successfully
	}

}
