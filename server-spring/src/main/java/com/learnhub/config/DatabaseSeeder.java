package com.learnhub.config;

import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.usermanagement.entity.User;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.entity.enums.EnergyLevel;
import com.learnhub.usermanagement.entity.enums.UserRole;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import com.learnhub.usermanagement.repository.UserRepository;
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

import java.io.File;
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
            logger.warn("Dataset CSV or PDF directory not found. Creating demo data instead.");
            // Fallback to demo data
            PDFDocument pdfDocument = createPlaceholderPDF();
            createDemoActivities(pdfDocument.getId());
        }

        // Create admin user
        createAdminUser();

        logger.info("Database seeding completed successfully!");
    }

    private void loadDatasetFromCSV(Path csvPath, Path pdfDir) {
        try (FileReader reader = new FileReader(csvPath.toFile());
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build())) {

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
                    activity.setAgeMin(Integer.parseInt(record.get("age_min")));
                    activity.setAgeMax(Integer.parseInt(record.get("age_max")));
                    activity.setFormat(ActivityFormat.fromValue(record.get("format")));
                    activity.setBloomLevel(BloomLevel.fromValue(record.get("bloom_level")));
                    activity.setDurationMinMinutes(Integer.parseInt(record.get("duration_min_minutes")));
                    activity.setDurationMaxMinutes(Integer.parseInt(record.get("duration_max_minutes")));
                    activity.setMentalLoad(EnergyLevel.fromValue(record.get("mental_load")));
                    activity.setPhysicalEnergy(EnergyLevel.fromValue(record.get("physical_energy")));
                    activity.setPrepTimeMinutes(Integer.parseInt(record.get("prep_time_minutes")));
                    activity.setCleanupTimeMinutes(Integer.parseInt(record.get("cleanup_time_minutes")));

                    // Parse pipe-delimited resources and topics
                    String resourcesStr = record.get("resources_needed");
                    activity.setResourcesNeeded(parseDelimitedList(resourcesStr));

                    String topicsStr = record.get("topics");
                    activity.setTopics(parseDelimitedList(topicsStr));

                    activity.setDocumentId(pdfDocument.getId());
                    activity.setCreatedAt(LocalDateTime.now());

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
        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private PDFDocument createPlaceholderPDF() {
        long existingDocuments = pdfDocumentRepository.count();
        if (existingDocuments > 0) {
            logger.info("PDF documents already exist. Using existing document.");
            return pdfDocumentRepository.findAll().get(0);
        }

        PDFDocument document = new PDFDocument();
        document.setFilename("demo_activities_placeholder.pdf");
        document.setFilePath(Paths.get(pdfStoragePath, "demo_activities_placeholder.pdf").toString());
        document.setFileSize(1024L);
        document.setExtractedFields("{}");
        document.setConfidenceScore("0.95");
        document.setExtractionQuality("high");
        document.setCreatedAt(LocalDateTime.now());

        document = pdfDocumentRepository.save(document);
        logger.info("Created placeholder PDF document with ID: {}", document.getId());
        return document;
    }

    private void createDemoActivities(Long documentId) {
        List<Activity> activities = Arrays.asList(
                createActivity("Binary Cards", "Learn binary number representation using cards",
                        8, 12, ActivityFormat.UNPLUGGED, BloomLevel.UNDERSTAND, 30, 45,
                        EnergyLevel.MEDIUM, EnergyLevel.LOW, Arrays.asList("handouts"),
                        Arrays.asList("patterns", "abstraction"), documentId),

                createActivity("Robot Commands", "Program a 'robot' classmate using simple commands",
                        6, 10, ActivityFormat.UNPLUGGED, BloomLevel.APPLY, 20, 30,
                        EnergyLevel.LOW, EnergyLevel.HIGH, Arrays.asList("stationery"),
                        Arrays.asList("algorithms", "decomposition"), documentId),

                createActivity("Sorting Network", "Learn sorting algorithms through physical activity",
                        10, 14, ActivityFormat.UNPLUGGED, BloomLevel.ANALYZE, 45, 60,
                        EnergyLevel.MEDIUM, EnergyLevel.HIGH, Arrays.asList("handouts"),
                        Arrays.asList("algorithms", "patterns"), documentId),

                createActivity("Scratch Animation", "Create animated stories using block-based coding",
                        8, 13, ActivityFormat.DIGITAL, BloomLevel.CREATE, 60, 90,
                        EnergyLevel.MEDIUM, EnergyLevel.LOW, Arrays.asList("computers", "tablets"),
                        Arrays.asList("algorithms", "decomposition"), documentId),

                createActivity("Pixel Art", "Design images by coloring grid squares",
                        7, 11, ActivityFormat.HYBRID, BloomLevel.APPLY, 30, 45,
                        EnergyLevel.LOW, EnergyLevel.LOW, Arrays.asList("handouts", "tablets"),
                        Arrays.asList("abstraction", "patterns"), documentId));

        activityRepository.saveAll(activities);
        logger.info("Created {} demo activities", activities.size());
    }

    private Activity createActivity(String name, String description, int ageMin, int ageMax,
            ActivityFormat format, BloomLevel bloomLevel,
            int durationMin, int durationMax,
            EnergyLevel mentalLoad, EnergyLevel physicalEnergy,
            List<String> resources, List<String> topics,
            Long documentId) {
        Activity activity = new Activity();
        activity.setName(name);
        activity.setDescription(description);
        activity.setSource("Demo Dataset");
        activity.setAgeMin(ageMin);
        activity.setAgeMax(ageMax);
        activity.setFormat(format);
        activity.setBloomLevel(bloomLevel);
        activity.setDurationMinMinutes(durationMin);
        activity.setDurationMaxMinutes(durationMax);
        activity.setMentalLoad(mentalLoad);
        activity.setPhysicalEnergy(physicalEnergy);
        activity.setPrepTimeMinutes(5);
        activity.setCleanupTimeMinutes(5);
        activity.setResourcesNeeded(resources);
        activity.setTopics(topics);
        activity.setDocumentId(documentId);
        activity.setCreatedAt(LocalDateTime.now());
        return activity;
    }

    private void createAdminUser() {
        String adminEmail = "admin@learnhub.com";

        if (userRepository.existsByEmail(adminEmail)) {
            logger.info("Admin user already exists");
            return;
        }

        String password = generateRandomPassword();

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
        logger.info("Password: {}", password);
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
