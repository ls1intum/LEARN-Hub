package com.learnhub.config;

import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.entity.enums.DocumentType;
import com.learnhub.activitymanagement.entity.enums.EnergyLevel;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import com.learnhub.usermanagement.entity.User;
import com.learnhub.usermanagement.entity.enums.UserRole;
import com.learnhub.usermanagement.repository.UserRepository;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
	private ActivityRepository activityRepository;

	@Autowired
	private PDFDocumentRepository pdfDocumentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${pdf.storage.path:/app/data/pdfs}")
	private String pdfStoragePath;

	@Value("${app.initial-admin-email:}")
	private String initialAdminEmail;

	@Value("${app.initial-admin-password:}")
	private String initialAdminPassword;

	@Override
	public void run(String... args) throws Exception {
		logger.info("Starting database seeding...");

		// Check if data already exists
		long existingActivities = activityRepository.count();

		if (existingActivities > 0) {
			logger.info("Database already contains {} activities. Skipping seeding.", existingActivities);
			return;
		}

		// Try to load full dataset from CSV
		Path datasetCsv = Paths.get("../dataset/dataset.csv");
		Path pdfDir = Paths.get("../dataset/pdfs");

		if (Files.exists(datasetCsv) && Files.isDirectory(pdfDir)) {
			logger.info("Found dataset CSV and PDF directory. Loading full dataset...");
			loadDatasetFromCSV(datasetCsv, pdfDir);
		} else {
			logger.warn("Dataset CSV or PDF directory not found. Skipping activity seeding.");
		}

		// Create admin user
		createAdminUser();

		logger.info("Database seeding completed successfully!");
	}

	private void loadDatasetFromCSV(Path csvPath, Path pdfDir) {
		try (FileReader reader = new FileReader(csvPath.toFile());
				CSVParser csvParser = new CSVParser(reader,
						CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

			int count = 0;
			for (CSVRecord record : csvParser) {
				try {
					String filename = record.get("filename");
					Path pdfPath = pdfDir.resolve(filename);

					if (!Files.exists(pdfPath)) {
						logger.warn("PDF file not found: {}. Skipping activity.", filename);
						continue;
					}

					logger.info("Processing activity: {}", record.get("name"));

					// Read PDF file
					byte[] pdfContent = Files.readAllBytes(pdfPath);

					// Store PDF document
					PDFDocument pdfDocument = new PDFDocument();
					pdfDocument.setFilename(filename);
					pdfDocument.setFilePath(Paths.get(pdfStoragePath, filename).toString());
					pdfDocument.setFileSize((long) pdfContent.length);
					pdfDocument.setExtractedFields("{}");
					pdfDocument.setConfidenceScore("1.0");
					pdfDocument.setExtractionQuality("manual");
					pdfDocument.setType(DocumentType.SOURCE_PDF);
					pdfDocument.setCreatedAt(LocalDateTime.now());
					pdfDocument = pdfDocumentRepository.save(pdfDocument);

					// Optionally save PDF to storage path
					try {
						Path storagePath = Paths.get(pdfStoragePath);
						Files.createDirectories(storagePath);
						Files.write(storagePath.resolve(filename), pdfContent);
					} catch (IOException e) {
						logger.warn("Could not save PDF to storage path: {}", e.getMessage());
					}

					// Parse and create activity
					Activity activity = new Activity();
					activity.setName(record.get("name"));
					activity.setDescription(record.get("description"));
					activity.setSource(record.get("source"));
					activity.setAgeMin(Integer.parseInt(record.get("ageMin")));
					activity.setAgeMax(Integer.parseInt(record.get("ageMax")));
					activity.setFormat(ActivityFormat.fromValue(record.get("format")));
					activity.setBloomLevel(BloomLevel.fromValue(record.get("bloomLevel")));
					activity.setDurationMinMinutes(Integer.parseInt(record.get("durationMinMinutes")));
					activity.setDurationMaxMinutes(Integer.parseInt(record.get("durationMaxMinutes")));
					activity.setMentalLoad(EnergyLevel.fromValue(record.get("mentalLoad")));
					activity.setPhysicalEnergy(EnergyLevel.fromValue(record.get("physicalEnergy")));
					activity.setPrepTimeMinutes(Integer.parseInt(record.get("prepTimeMinutes")));
					activity.setCleanupTimeMinutes(Integer.parseInt(record.get("cleanupTimeMinutes")));

					// Parse pipe-delimited resources and topics
					String resourcesStr = record.get("resourcesNeeded");
					activity.setResourcesNeeded(parseDelimitedList(resourcesStr));

					String topicsStr = record.get("topics");
					activity.setTopics(parseDelimitedList(topicsStr));

					activity.setCreatedAt(LocalDateTime.now());

					activity.getDocuments().add(pdfDocument);

					activityRepository.save(activity);
					count++;

				} catch (Exception e) {
					logger.error("Error processing activity from CSV: {}", e.getMessage(), e);
				}
			}

			logger.info("Successfully imported {} activities from dataset", count);

		} catch (IOException e) {
			logger.error("Error reading dataset CSV: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to load dataset", e);
		}
	}

	private List<String> parseDelimitedList(String value) {
		if (value == null || value.trim().isEmpty()) {
			return new ArrayList<>();
		}
		return Arrays.stream(value.split("\\|")).map(String::trim).filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
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
