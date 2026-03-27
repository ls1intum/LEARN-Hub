# LEARN-Hub Server

Spring Boot REST API server for educational activity recommendations.

## Technology Stack

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
- Docker (for PostgreSQL and containerised deployment)
- LibreOffice (required for local DOCX generation via `soffice`)

On macOS, install it with:

```bash
brew install --cask libreoffice
```

The local development profile uses `/Applications/LibreOffice.app/Contents/MacOS/soffice`.

### Local Development

1. **Start PostgreSQL Database**:
```bash
docker compose -f docker/compose.yml up postgres -d
```

2. **Run Database Migrations**:
```bash
make db-migrate
```

3. **Run the Application**:
```bash
make dev
```

The server will start on `http://localhost:5001`

## Development Commands

```bash
make dev          # Run development server
make build        # Build the application JAR
make test         # Run tests
make lint         # Check code style (Spotless)
make format       # Format code (Spotless)
make db-migrate   # Run Flyway migrations
make db-clean     # Clean database (WARNING: deletes all data)
```

## Database Seeding

The seeder automatically loads the dataset from the repository if available:

**Option 1: Full Dataset** — Requires `dataset/dataset.csv` and `dataset/pdfs/` to be present.

**Option 2: Demo Data** — Falls back to 5 sample activities if dataset files are not found.

To enable seeding, set in `.env`:
```properties
DB_SEED_ENABLED=true
```

Then start the application. The seeder will create an admin user with auto-generated credentials (printed in logs).

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new teacher
- `POST /api/auth/login` - Login with email/password
- `POST /api/auth/verify-code` - Verify email code and get JWT token
- `POST /api/auth/request-verification-code` - Request new verification code
- `POST /api/auth/refresh` - Refresh JWT token
- `GET /api/auth/me` - Get current user profile
- `PUT /api/auth/me` - Update current user profile

### Activities
- `GET /api/activities/` - List activities with filtering
- `GET /api/activities/{id}` - Get activity by ID
- `POST /api/activities/create` - Create new activity (admin only)
- `PUT /api/activities/{id}` - Update activity (admin only)
- `DELETE /api/activities/{id}` - Delete activity (admin only)
- `GET /api/activities/recommendations` - Get recommendations
- `POST /api/activities/lesson-plan` - Generate lesson plan

### History & Favourites
- `GET /api/history/search` - Get user's search history
- `DELETE /api/history/search/{id}` - Delete search history entry
- `POST /api/history/favourites/activities` - Save activity as favourite
- `GET /api/history/favourites/activities` - Get activity favourites
- `DELETE /api/history/favourites/activities/{id}` - Delete activity favourite
- `POST /api/history/favourites/lesson-plans` - Save lesson plan as favourite
- `GET /api/history/favourites/lesson-plans` - Get lesson plan favourites

### Documents
- `POST /api/activities/upload-pdf-draft` - Upload PDF and extract metadata via LLM
- `GET /api/documents/{id}` - Download PDF file
- `GET /api/documents/{id}/info` - Get PDF metadata

### System
- `GET /api/hello` - Health check
- `GET /api/meta/field-values` - Get available enum values for all form fields
- `GET /api/openapi/swagger` - Swagger UI

## Configuration

Key environment variables (see `example.env` for full list):

| Variable | Description |
|----------|-------------|
| `POSTGRES_DB_URI` | PostgreSQL JDBC connection string |
| `JWT_SECRET_KEY` | JWT token signing key |
| `LLM_BASE_URL` | Ollama API base URL |
| `LLM_API_KEY` | Ollama API key |
| `LLM_MODEL_NAME` | LLM model name |
| `EMAIL_ADDRESS` | SMTP from address |
| `EMAIL_USERNAME` | SMTP username |
| `EMAIL_PASSWORD` | SMTP password |
| `PDF_PATH` | Path for PDF file storage |
| `DB_SEED_ENABLED` | Enable database seeding on startup |

## Testing

```bash
make test
```

## Docker Deployment

The server container already installs LibreOffice (`libreoffice-writer`), so no extra host dependency is needed when running the server in Docker.

```bash
# Build and run entire stack
docker compose -f docker/compose.yml up --build

# Production (pre-built images)
docker compose -f docker/compose.prod.yml up -d
```
