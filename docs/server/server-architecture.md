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

**Spring Boot**: Enterprise-grade framework with extensive ecosystem. Spring AI provides LLM integration; Spring Security handles JWT authentication; Spring Data JPA manages persistence with Hibernate ORM.

**PostgreSQL**: Production-ready relational database with ACID compliance, supporting concurrent access and complex relationships between activities, topics, and user preferences.

**Java 21**: Modern Java with virtual threads, records, and pattern matching support. Compile-time type safety prevents runtime errors common in dynamic languages.

## Authentication Architecture

The system implements JWT-based authentication:

**All Users**: Email/password authentication returns JWT access tokens and refresh tokens. Access tokens (short-lived) limit exposure; refresh tokens maintain sessions.

**Teacher Registration**: Email verification codes (6-digit, 10-minute expiry, 3-attempt limit) reduce onboarding friction.

**Spring Security**: Configures endpoint access rules, JWT filter chain, and role-based authorisation (`TEACHER` and `ADMIN` roles).

## Service Architecture

The service layer implements business logic through specialised services:

- **RecommendationService**: Orchestrates the recommendation engine, translating API criteria to scoring inputs
- **ScoringEngineService**: Implements category-based scoring algorithm
- **ActivityService**: Handles activity CRUD operations and PDF upload workflow
- **PDFService**: Manages document storage, retrieval, and lesson plan generation
- **AuthService**: Handles user authentication, registration, and JWT token management
- **EmailService**: Manages email delivery for verification codes
- **UserSearchHistoryService & UserFavouritesService**: Track user preferences and interactions

Services use constructor injection following the single-responsibility principle.

## LLM Integration

The system integrates with Ollama via Spring AI for automated PDF content extraction. Administrators upload PDFs; the system extracts structured activity metadata (name, description, age range, Bloom level, topics, resources) via the configured LLM model. The extracted data is presented for review and approval before persistence.

## Performance Optimisation

The recommendation engine implements two-stage scoring to achieve sub-three-second response times:

1. **Stage 1**: Score all filtered activities by age, Bloom alignment, and topic
2. **Stage 2**: Re-score only top candidates with duration and break insertion logic

Hard filters eliminate incompatible activities before scoring.

## Deployment Architecture

The application is containerised using Docker with multi-stage builds. The build stage uses Maven to produce a fat JAR; the production stage runs a minimal Eclipse Temurin JRE 21 image. Docker Compose orchestrates three containers (client, server, database) via an internal bridge network with health checks ensuring proper startup sequencing.

The server integrates with two external services: an Ollama LLM API for PDF extraction via HTTPS, and an SMTP server for email delivery via STARTTLS encryption on port 587.

## Data Persistence

PostgreSQL manages data persistence with ACID compliance. Spring Data JPA with Hibernate provides object-relational mapping; Flyway manages schema migrations with version control. Core entities (User, Activity, PDFDocument, UserSearchHistory, UserFavourites, VerificationCode) structure the data model, with referential integrity enforced through JPA relationships and foreign key constraints.

## API Naming Convention

All JSON request and response fields follow **camelCase** convention (REST API best practice), matching the natural Java naming of DTO fields. Spring's default Jackson serialisation handles the conversion automatically.

## Configuration

Spring Boot's `application.properties` maps environment variables to configuration beans. Key settings include database connection, JWT secret, LLM integration, email service, CORS allowed origins, and PDF storage path.
