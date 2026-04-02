package com.learnhub.config;

import com.learnhub.usermanagement.entity.User;
import com.learnhub.usermanagement.entity.enums.UserRole;
import com.learnhub.usermanagement.repository.UserRepository;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.db-seed.enabled", havingValue = "true", matchIfMissing = false)
public class DatabaseSeeder implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.initial-admin-email:}")
	private String initialAdminEmail;

	@Value("${app.initial-admin-password:}")
	private String initialAdminPassword;

	@Override
	public void run(String... args) throws Exception {
		logger.info("Starting database seeding...");
		createAdminUser();
		logger.info("Database seeding completed successfully!");
	}

	private void createAdminUser() {
		String adminEmail = initialAdminEmail != null && !initialAdminEmail.isBlank()
				? initialAdminEmail
				: "admin@learnhub.com";

		if (userRepository.existsByEmail(adminEmail)) {
			logger.info("Admin user already exists");
			return;
		}

		boolean usesInitialPassword = initialAdminPassword != null && !initialAdminPassword.isBlank();
		String password = usesInitialPassword ? initialAdminPassword : generateRandomPassword();

		User admin = new User();
		admin.setEmail(adminEmail);
		admin.setFirstName("Admin");
		admin.setLastName("User");
		admin.setRole(UserRole.ADMIN);
		admin.setPasswordHash(passwordEncoder.encode(password));

		userRepository.save(admin);

		logger.info("=".repeat(60));
		logger.info("ADMIN CREDENTIALS");
		logger.info("=".repeat(60));
		logger.info("Email: {}", adminEmail);
		if (usesInitialPassword) {
			logger.info("Password: [from INITIAL_ADMIN_PASSWORD]");
		} else {
			logger.info("Password: {}", password);
		}
		logger.info("=".repeat(60));
	}

	private String generateRandomPassword() {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
		StringBuilder password = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 12; i++) {
			password.append(chars.charAt(random.nextInt(chars.length())));
		}
		return password.toString();
	}
}
