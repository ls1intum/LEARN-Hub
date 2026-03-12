# Server Development and Deployment

## Quick Start

```bash
cd server/
make db-migrate  # Run Flyway database migrations
make dev         # Start development server (port 5001)
```

## Development Commands

### Application
- `make dev` - Start development server with Spring Boot DevTools
- `make build` - Build the application JAR

### Code Quality
- `make format` - Format code with Spotless (Google Java Style)
- `make lint` - Check code style with Spotless

### Testing
- `make test` - Run all tests with JUnit

### Database
- `make db-migrate` - Apply Flyway migrations
- `make db-clean` - Clean database (WARNING: deletes all data)

## Testing Strategy

The test suite uses JUnit 5 and Mockito.

**Running Tests**:
```bash
make test
```

## Code Quality

- **Spotless**: Java code formatter using Google Java Style
- **Constructor injection**: All Spring beans use constructor injection (no `@Autowired` field injection)

**Pre-commit Workflow**:
```bash
make format && make lint && make test
```

## Database Management

Flyway manages version-controlled schema migrations located in `src/main/resources/db/migration/`:

```bash
make db-migrate   # Apply all pending migrations
make db-clean     # Revert all migrations (development only)
```

**Migration naming**: `V{version}__{description}.sql` (e.g., `V1__Initial_schema.sql`)

## Production Deployment

### Containerisation

Multi-stage Docker build:
- **Build Stage**: Maven builds a fat JAR (`./mvnw clean package -DskipTests`)
- **Production Stage**: Eclipse Temurin JRE 21 slim image
- **Non-root User**: Application runs as non-root for security

Health check endpoint: `/api/hello` (used by Docker health checks)

### Container Orchestration

Docker Compose manages the application:

```bash
# Development
docker compose -f docker/compose.yml up --build -d

# Production (pre-built images from GitHub Container Registry)
docker compose -f docker/compose.prod.yml up -d

# View logs
docker compose -f docker/compose.yml logs -f server
```

### Configuration

Spring Boot maps environment variables to configuration properties:

**Required Variables**:
- `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL_NAME` - Ollama LLM service
- `JWT_SECRET_KEY` - Security key for JWT token signing
- `POSTGRES_DB_URI` - PostgreSQL JDBC connection string
- `EMAIL_*` - Email service configuration
- `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` - Bootstrap admin (optional)

The `DB_SEED_ENABLED` variable controls automatic database seeding on startup.

## Related Documentation

- [Server Architecture](server-architecture.md) - Architectural decisions and design patterns
- [API Documentation](api.md) - REST API endpoints and patterns
- [Client Deployment](../client/client-cicd.md) - Client deployment procedures
