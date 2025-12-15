# Server Architecture

## Overview

The LEARN-Hub server implements a Flask-based REST API for educational activity recommendations through category-based scoring. The architecture emphasises modularity, type safety, and scalability with clear separation of concerns across application layers.

## Design Goals and Constraints

The server architecture responds to explicit quality attributes and constraints:

- **QA1 / QA8**: RESTful API with OpenAPI specifications enables external integration
- **QA3**: Category-based scoring provides transparent scoring explanations
- **QA4**: Graceful degradation ensures core recommendation functionality when optional features fail
- **QA5 / QA6**: Two-stage pipeline limits combinatorial search, targeting sub-three-second response times
- **QA7**: Configuration modules isolate constants for extensibility

The implementation satisfies deployment constraints (C1: web-based, C2: containerised, C3: secure authentication, C4: Bloom's Taxonomy alignment, C5: OpenAPI documentation).

## Layered Architecture

The server follows a classical layered pattern:

- **API Layer** (`app/api/`): Exposes RESTful endpoints via Flask-OpenAPI3 with automated documentation from Pydantic models
- **Service Layer** (`app/services/`): Encapsulates business logic; services use dependency injection for testability
- **Core Engine** (`app/core/`): Recommendation algorithm isolated as a standalone package
- **Data Access Layer** (`app/db/`): SQLAlchemy ORM abstracts database operations

## Technology Rationale

**Flask**: Lightweight framework with explicit architectural control, appropriate for prototype scope. Flask-OpenAPI3 eliminates manual API documentation maintenance.

**PostgreSQL**: Production-ready relational database with ACID compliance, supporting concurrent access and complex relationships between activities, topics, and user preferences.

**Python 3.13**: Modern type hints and performance improvements support research prototype development without backward compatibility constraints.

## Authentication Architecture

The system implements dual authentication to balance security with usability:

**Admin**: Email/password with JWT tokens provide robust security for users with full system access.

**Teachers**: Email verification codes (6-digit, 10-minute expiry, 3-attempt limit) reduce onboarding friction. This trades slightly elevated security risk for significantly improved usability in time-constrained classrooms.

**Token Management**: Access tokens (15 minutes) limit exposure; refresh tokens (30 days) maintain sessions. Stateless JWT design eliminates server-side session storage.

## Service Architecture

The service layer implements business logic through specialised services:

- **RecommendationService**: Orchestrates the recommendation engine, translating API requests to core engine inputs
- **PDFService**: Manages document storage, retrieval, and lesson plan generation
- **UserService**: Handles user management, authentication, and profile updates
- **EmailService**: Manages email delivery with environment-specific configuration
- **UserSearchHistoryService & UserFavouritesService**: Track user preferences and interactions

Services use explicit dependency injection to maintain clarity and facilitate testing.

## LLM Integration

The system integrates with Ollama via LangChain for automated PDF content extraction, using the Qwen 3 (30b-a3b) model. Administrators upload PDFs; the system extracts structured activity metadata (name, description, age range, Bloom level, topics, resources) for review and approval before persistence. This approach trades external service dependency for reduced manual data entry burden.

Lesson plan PDFs are generated programmatically via ReportLab, creating summary pages and merging them with individual activity PDFs. This deterministic generation ensures repeatable evaluation.

## Performance Optimisation

The recommendation engine implements two-stage scoring to achieve sub-three-second response times:

1. **Stage 1**: Score all filtered activities by age, Bloom alignment, and topic (omitting expensive duration calculation)
2. **Stage 2**: Re-score only top 25 candidates with duration and break insertion logic

This candidate limit reduces combinatorial complexity from O(n!) to O(25^k), where k is the sequence length. Hard filters eliminate incompatible activities before scoring. The trade-off accepts sub-optimal recommendations for responsive interaction.

## Deployment Architecture

The application is containerised using Docker with multi-stage builds. The build stage uses the `uv` tool for dependency installation; the production stage runs a minimal Python image with Gunicorn as the WSGI server. Docker Compose orchestrates three containers (client, server, database) via an internal bridge network with health checks ensuring proper startup sequencing.

The server integrates with two external services: an Ollama LLM API (on a TUM GPU server) for PDF extraction via HTTPS, and an SMTP server for email delivery via STARTTLS encryption on port 587.

## Data Persistence

PostgreSQL manages data persistence with ACID compliance and support for complex relationships. SQLAlchemy provides object-relational mapping; Alembic manages schema migrations with version control. Five core entities (User, Activity, Document, UserSearchHistory, UserFavourites) structure the data model, with referential integrity enforced through foreign key constraints preventing orphaned records.

## Configuration

The system uses Pydantic Settings for type-safe, environment-variable-based configuration. The `ENVIRONMENT` variable enables runtime behavior adaptation across local, staging, and production contexts without code changes.
