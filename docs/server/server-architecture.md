# Server Architecture

## Overview

The LEARN-Hub server implements a Spring Boot REST API for educational activity recommendations through category-based scoring. The architecture emphasises modularity, type safety, and scalability with clear separation of concerns across application layers.

## Design Goals and Constraints

The server architecture responds to explicit quality attributes and constraints:

- **QA1 / QA8**: RESTful API with OpenAPI specifications enables external integration
- **QA3**: Category-based scoring provides transparent scoring explanations
- **QA4**: Graceful degradation ensures core recommendation functionality when optional features fail
- **QA5 / QA6**: Two-stage pipeline limits combinatorial search, targeting sub-three-second response times
- **QA7**: Configuration modules isolate constants for extensibility

The implementation satisfies deployment constraints (C1: web-based, C2: containerised, C3: secure authentication, C4: Bloom's Taxonomy alignment, C5: OpenAPI documentation).

## Layered Architecture

The server follows a domain-driven layered architecture:

- **Controller Layer** (`*/controller/`): Exposes RESTful endpoints via Spring MVC with automatic OpenAPI documentation from SpringDoc
- **Service Layer** (`*/service/`): Encapsulates business logic; services use constructor injection for testability
- **Repository Layer** (`*/repository/`): Spring Data JPA repositories abstract database operations
- **Entity Layer** (`*/entity/`): JPA entities mapping to PostgreSQL tables
- **DTO Layer** (`*/dto/`): Request/response data transfer objects with camelCase JSON naming

The code is organised into three main domains:
1. **Activity Management** (`activitymanagement`) - Activities, recommendations, scoring
2. **User Management** (`usermanagement`) - Users, authentication, favourites, history
3. **Document Management** (`documentmanagement`) - PDF documents, LLM integration

## Technology Rationale

**Spring Boot**: Enterprise-grade framework with extensive ecosystem. Spring AI provides LLM integration via an OpenAI-compatible client; Spring Security and Spring Session handle cookie-based authentication; Spring Data JPA manages persistence with Hibernate ORM.

**PostgreSQL**: Production-ready relational database with ACID compliance, supporting concurrent access and complex relationships between activities, topics, and user preferences.

**Java 21**: Modern Java with virtual threads, records, and pattern matching support. Compile-time type safety prevents runtime errors common in dynamic languages.

## Authentication Architecture

The system implements Spring Security session-based authentication:

**All Users**: Successful login creates a server-side session persisted via Spring Session JDBC and identified by an `HttpOnly` session cookie. The SPA fetches a CSRF token and sends it on mutating requests.

**Teacher Registration**: Email verification codes (6-digit, 10-minute expiry, 3-attempt limit) reduce onboarding friction.

**Spring Security**: Configures endpoint access rules, session management, CSRF protection, and role-based authorisation (`TEACHER` and `ADMIN` roles).

## Service Architecture

The service layer implements business logic through specialised services:

- **RecommendationService**: Orchestrates the recommendation engine, translating API criteria to scoring inputs
- **ScoringEngineService**: Implements category-based scoring algorithm
- **ActivityService**: Handles activity CRUD operations and PDF upload workflow
- **ActivityExtractionService**: Coordinates LLM-based metadata extraction and normalisation for admin draft workflows
- **ActivityDraftService**: Manages the draft lifecycle and sequential markdown generation
- **PDFService**: Manages PDF document storage, retrieval, and text extraction
- **LLMService**: Wraps Spring AI chat and image clients; implements all prompt-based generation tasks
- **AdobePdfToDocxService**: Converts PDFs to DOCX via Adobe PDF Services (optional; gracefully disabled when credentials are absent)
- **MarkdownToPdfService / MarkdownToDocxService**: Convert stored markdown to downloadable PDF and DOCX respectively; DOCX conversion uses Adobe when available, falling back to LibreOffice
- **DocxCacheService**: Caches converted DOCX bytes to avoid redundant Adobe API calls
- **AuthService**: Handles user authentication, registration, and account lifecycle operations
- **EmailService**: Manages email delivery for verification codes
- **UserSearchHistoryService & UserFavouritesService**: Track user preferences and interactions

Services use constructor injection following the single-responsibility principle.

## LLM Integration

The system integrates with any OpenAI-compatible chat API via Spring AI for PDF-based content processing. The `LLMService` handles several document generation tasks, all driven by prompt templates in `src/main/resources/prompts/`:

- **Metadata extraction**: Administrators upload a PDF; the LLM extracts structured activity metadata (name, description, age range, Bloom level, topics, resources). Extracted data is presented for review before persistence.
- **Artikulationsschema**: Generates or normalises a lesson-phase schema (AVIVA+ format) from PDF text and confirmed metadata.
- **Deckblatt**: Generates a cover-page markdown with activity overview, materials, and learning objectives.
- **Hintergrundwissen**: Generates teacher background-knowledge markdown from PDF content.
- **Übung & Lösungsblatt**: Generates paired exercise and solution sheets in a single LLM call.
- **Tafelbild**: Generates a board-diagram image using the optional OpenAI image model.

`[[IMAGE_PLACEHOLDER: ...]]` markers emitted by the exercise LLM are replaced with images generated by the optional OpenAI image model (`LLMService.generateImageMarkdown`). If the image model is not configured, placeholders are preserved in the markdown output.

## Performance Optimisation

The recommendation engine implements two-stage scoring to achieve sub-three-second response times:

1. **Stage 1**: Score all filtered activities by age, Bloom alignment, and topic
2. **Stage 2**: Re-score only top candidates with duration and break insertion logic

Hard filters eliminate incompatible activities before scoring.

## Deployment Architecture

The application is containerised using Docker with multi-stage builds. The build stage uses Maven to produce a fat JAR; the production stage runs a minimal Eclipse Temurin JRE 21 image. Docker Compose orchestrates three containers (client, server, database) via an internal bridge network with health checks ensuring proper startup sequencing.

The server integrates with up to four external services: an OpenAI-compatible text LLM for PDF extraction and document generation; an optional OpenAI image model for exercise illustration generation; an optional Adobe PDF Services REST API for PDF-to-DOCX conversion; and an SMTP server for email delivery via STARTTLS on port 587. All external calls use HTTPS.

## Data Persistence

PostgreSQL manages data persistence with ACID compliance. Spring Data JPA with Hibernate provides object-relational mapping; Flyway manages schema migrations with version control. Core entities (User, Activity, PDFDocument, UserSearchHistory, UserFavourites, VerificationCode) structure the data model, with referential integrity enforced through JPA relationships and foreign key constraints.

## API Naming Convention

All JSON request and response fields follow **camelCase** convention (REST API best practice), matching the natural Java naming of DTO fields. Spring's default Jackson serialisation handles the conversion automatically.

## Configuration

Spring Boot's `application.properties` maps environment variables to configuration beans. Key settings include database connection, session lifetime, LLM integration, email service, CORS allowed origins, and PDF storage path.
