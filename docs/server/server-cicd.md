# Server Development and Deployment

## Overview

This document outlines the development workflow, testing strategy, and deployment procedures for the LEARN-Hub server.

## Development Environment

### Package Management with `uv`

**Critical Requirement**: All Python operations must use `uv` as the package and task manager. Direct use of `pip` or `python` is prohibited to ensure consistent dependency resolution and virtual environment management.

All Python tool execution must be prefixed with `uv run`:

```bash
uv run python <script>
uv run pytest
uv run alembic <command>
```

### Quick Start

```bash
cd server/
make setup      # Install dependencies via uv sync
make db-setup   # Run database migrations
make restore    # Load initial dataset
make dev        # Start development server
```

### Environment Configuration

Required environment variables:
- `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL_NAME` - LLM service integration
- `FLASK_SECRET_KEY`, `JWT_SECRET_KEY` - Security keys
- `SQLALCHEMY_DATABASE_URI` - PostgreSQL connection
- `SMTP_*` - Email service configuration
- `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` - Bootstrap admin

Environment variables enable the same codebase to run across different environments without modification.

```bash
cp example.env .env
# Edit .env with appropriate values
```

### Development Commands

**Application**:
- `make dev` - Development server (port 5001)
- `make run` - Production-like server (Gunicorn)

**Code Quality**:
- `make format` - Format with Black (120 char lines)
- `make lint` - Check with Ruff
- `make lint-fix` - Auto-fix linting issues

**Testing**:
- `make test` - All tests with coverage
- `make test-unit` - Unit tests only
- `make test-integration` - Integration tests

**Database**:
- `make db-setup` - Apply migrations
- `make db-init` - Fresh database
- `make db-reset` - Reset (development only)
- `make backup` / `make restore` - Backup/restore

## Testing Strategy

The testing strategy prioritizes high-value tests over comprehensive coverage:

**Targets**:
- Execution time: <4 seconds
- Test count: <100 tests
- Coverage: >50% on critical business logic
- Focus: User-facing features and error paths

**Test Organization**:
- **Unit Tests** (`tests/unit/`): Isolated components with mocked dependencies
- **Integration Tests** (`tests/integration/`): Database operations and API endpoints with in-memory SQLite

Tests use isolated environments (in-memory database, test variables, mocked external services) to ensure reproducibility.

**Running Tests**:
```bash
make test                    # All tests with coverage
make test-unit               # Fast unit tests only
uv run pytest                # Direct execution
uv run pytest tests/unit/    # Specific directory
```

## Code Quality

**Black** (formatter): 120-character lines, automatic formatting
**Ruff** (linter): Fast Python linter combining multiple tools
**Type Hints**: Required for all functions, use modern syntax (`list[str]`)

**Pre-commit Workflow**:
```bash
make format && make lint && make test
```

The project uses Python 3.13 features including enhanced type hints, improved error messages, and match/case pattern matching.

## Database Management

Database schema evolution uses Alembic for version-controlled migrations:

**Common Operations**:
```bash
uv run alembic revision --autogenerate -m "Description"  # Create migration
uv run alembic upgrade head                               # Apply migrations
uv run alembic downgrade -1                               # Revert last
uv run alembic current                                    # Check status
```

Migrations are version-controlled Python scripts that can be applied (upgrade) or reversed (downgrade). Review auto-generated migrations before applying and never modify already-applied migrations.

**Workflows**:
```bash
# Development
make db-setup     # Apply migrations
make db-init      # Fresh database

# Production
make backup       # Create backup
make db-setup     # Apply migrations
make restore      # Rollback if needed
```

## Production Deployment

### Containerization

The application uses Docker with multi-stage builds:

**Build Stage**: Uses official `uv` image to install dependencies from `pyproject.toml` and `uv.lock`

**Production Stage**: Minimal Python slim image with Gunicorn as WSGI server, runs as non-root user

This approach minimizes image size while maintaining reproducibility.

### Container Orchestration

Docker Compose manages the multi-container application (PostgreSQL database, Flask server, React client):

```bash
# Development
docker compose up --build -d

# Production (uses pre-built images from GitHub Container Registry)
docker compose -f compose.prod.yml up -d

# View logs
docker compose logs -f server
```

### Health Checks

The `/api/hello` endpoint provides health checking for container orchestration. The `/api/openapi` endpoint serves interactive API documentation. Application logs are captured by the container orchestration system.

## Related Documentation

- [Server Architecture](server-architecture.md) - Architectural decisions and design patterns
- [API Documentation](api.md) - REST API endpoints and data models
- [Client Deployment](../client/client-cicd.md) - Client deployment procedures
