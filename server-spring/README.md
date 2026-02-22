# LEARN-Hub Server Migration to Spring Boot

This document describes the Spring Boot implementation of the LEARN-Hub server, which replaces the previous Flask implementation.

## Overview

The server has been migrated from Flask/Python to Spring Boot/Java while maintaining full API compatibility and all existing functionality.

## Technology Stack

**Server**:
- Java 21 with Spring Boot 3.4.1
- Spring Data JPA with Hibernate ORM
- PostgreSQL 17+ for data persistence
- Flyway for database migrations
- Spring AI with Ollama for LLM integration
- Spring Security with JWT authentication
- Maven for dependency management

## Quick Start

### Prerequisites

- Java 21 or higher
- **Maven Wrapper included** - no need to install Maven separately
- Docker (for PostgreSQL and containerized deployment)
- PostgreSQL 17+ (if running locally without Docker)

### Environment Variables

Create a `.env` file in the project root based on `example.env`:

```bash
cp example.env .env
```

Update the following variables:
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` - Database configuration
- `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL_NAME` - LLM configuration for activity extraction
- `JWT_SECRET_KEY` - JWT token signing key
- `EMAIL_ADDRESS`, `EMAIL_USERNAME`, `EMAIL_PASSWORD` - Email configuration for verification
- `PDF_PATH` - Path for PDF storage
- `app.db-seed.enabled` - Set to `true` to enable database seeding

### Local Development Setup

1. **Start PostgreSQL Database**:
```bash
docker compose -f compose-spring.yml up postgres -d
```

2. **Run Database Migrations**:
```bash
cd server-spring
./mvnw flyway:migrate
# OR
make db-migrate
```

3. **Run the Application**:
```bash
./mvnw spring-boot:run
# OR
make dev
```

The server will start on `http://localhost:5001`

### Database Seeding

The seeder automatically loads the **full dataset** from the repository if available:

**Option 1: Full Dataset (37 activities)**
- Requires `dataset/dataset.csv` and `dataset/pdfs/` to be present
- Automatically loads all 37 activities with their PDF files
- Copies PDFs to the configured storage path

**Option 2: Demo Data (5 activities)**
- Falls back if dataset files not found
- Creates placeholder PDF and 5 sample activities

To enable seeding, add to your `.env`:

```properties
app.db-seed.enabled=true
```

Then restart the application. The seeder will:
- Check for `dataset/dataset.csv` and load full dataset if present
- Otherwise create 5 demo activities
- Create an admin user with auto-generated credentials (printed in logs)

For production deployment, set `app.db-seed.enabled=false`.

### Maven Wrapper

The project includes Maven Wrapper, so you don't need to install Maven:

```bash
# Unix/Linux/Mac
./mvnw clean package
./mvnw spring-boot:run

# Windows
mvnw.cmd clean package
mvnw.cmd spring-boot:run
```

### Docker Deployment

Build and run the entire stack (PostgreSQL + Spring Boot server + React client):

```bash
docker compose -f compose-spring.yml up --build
```

Access the services:
- **Client**: http://localhost:3001
- **Server API**: http://localhost:5001
- **API Documentation**: http://localhost:5001/api/openapi/swagger

## Development Commands

```bash
cd server-spring/

make dev          # Run development server
make build        # Build the application
make test         # Run tests
make lint         # Check code quality
make format       # Format code
make db-migrate   # Run Flyway migrations
make db-clean     # Clean database
```

## API Endpoints

All Flask API endpoints have been migrated to Spring Boot with identical paths and functionality:

### Authentication
- `POST /api/auth/register` - Register new teacher
- `POST /api/auth/login` - Login with email/password or request verification code
- `POST /api/auth/verify-code` - Verify email code and get JWT token
- `POST /api/auth/request-verification-code` - Request new verification code

### Activities
- `GET /api/activities/` - List activities with filtering
- `GET /api/activities/{id}` - Get activity by ID
- `POST /api/activities/` - Create new activity (admin only)
- `PUT /api/activities/{id}` - Update activity (admin only)
- `DELETE /api/activities/{id}` - Delete activity (admin only)

### History & Favourites
- `GET /api/history/search` - Get user's search history
- `DELETE /api/history/search/{id}` - Delete search history entry
- `GET /api/history/favourites` - Get all favourites (optional type filter)
- `POST /api/history/favourites/activities` - Save activity as favourite
- `POST /api/history/favourites/lesson-plans` - Save lesson plan as favourite
- `DELETE /api/history/favourites/{id}` - Delete favourite

### Documents
- `POST /api/documents/upload_pdf` - Upload PDF document (admin only)
- `GET /api/documents/{id}` - Download PDF file
- `GET /api/documents/{id}/info` - Get PDF metadata

### Meta
- `GET /api/hello` - Health check
- `GET /api/meta/formats` - Get available activity formats
- `GET /api/meta/bloom-levels` - Get Bloom taxonomy levels
- `GET /api/meta/resources` - Get available resources
- `GET /api/meta/topics` - Get computational thinking topics
- `GET /api/meta/energy-levels` - Get energy levels

### Documentation
- `GET /api/openapi/swagger` - Swagger UI
- `GET /openapi.json` - OpenAPI specification

## Key Features

### 1. Spring AI Integration for Ollama

The LLM client has been migrated to use Spring AI's Ollama support:

```java
@Service
public class LLMService {
    @Autowired
    private OllamaChatModel ollamaChatModel;
    
    public Map<String, Object> extractActivityData(String pdfText) {
        // Uses Spring AI Ollama chat model
    }
}
```

### 2. Flyway Database Migrations

Database schema is managed with Flyway SQL migrations in `src/main/resources/db/migration/`:
- `V1__Initial_schema.sql` - Base schema with all tables

### 3. JWT Authentication with Spring Security

Spring Security handles authentication and authorization:
- JWT tokens for stateless authentication
- Role-based access control (TEACHER, ADMIN)
- BCrypt password hashing

### 4. JPA Entities

All SQLAlchemy models converted to JPA entities:
- `Activity`, `User`, `PDFDocument`, `VerificationCode`
- `UserSearchHistory`, `UserFavourites`

### 5. Database Seeding

CommandLineRunner implementation for database seeding:
- Controlled by `app.db-seed.enabled` property
- Creates demo activities and admin user
- Only runs if database is empty

## Configuration

### application.properties

Key configuration properties:

```properties
# Server
server.port=5001

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/learn_hub_activities
spring.jpa.hibernate.ddl-auto=validate

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Spring AI Ollama
spring.ai.ollama.base-url=${LLM_BASE_URL}
spring.ai.ollama.chat.model=${LLM_MODEL_NAME}

# JWT
jwt.secret=${JWT_SECRET_KEY}
jwt.expiration=86400000

# Email
spring.mail.host=${SMTP_SERVER}
spring.mail.port=${SMTP_PORT}
```

### Profiles

Different profiles can be used for different environments:
- `application.properties` - Default configuration
- `application-dev.properties` - Development overrides
- `application-prod.properties` - Production overrides

Activate profile with: `spring.profiles.active=dev`

## Testing

```bash
# Run all tests
make test

# Run integration tests
make test-integration
```

## Production Deployment

1. Build the application:
```bash
make build
```

2. Build Docker image:
```bash
make build-docker
```

3. Deploy with Docker Compose:
```bash
docker compose -f compose-spring.yml up -d
```

## Migration Notes

### API Compatibility

All existing Flask endpoints have been migrated with identical:
- URL paths
- Request/response formats
- Query parameters
- Authentication mechanisms

Client applications require no changes.

### Database Schema

The Flyway migration scripts recreate the exact database schema from Alembic migrations, ensuring data compatibility.

### Environment Variables

Environment variables remain compatible with the Flask version, with one addition:
- Previous: `SQLALCHEMY_DATABASE_URI` (PostgreSQL connection string)
- Spring Boot also accepts: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`

## Troubleshooting

### Database Connection Issues

Check PostgreSQL is running:
```bash
docker compose -f compose-spring.yml ps postgres
```

### Migration Failures

Clean and reapply migrations:
```bash
make db-clean
make db-migrate
```

### Port Already in Use

Change the server port in `.env`:
```bash
SERVER_PORT=5002
```

## Additional Documentation

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/index.html)
- [Spring AI](https://docs.spring.io/spring-ai/reference/)
- [Flyway](https://flywaydb.org/documentation/)
