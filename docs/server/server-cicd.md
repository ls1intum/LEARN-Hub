# Server Development and Deployment

## Quick Start

```bash
cd server/
make setup      # Install dependencies via uv sync
make db-setup   # Run database migrations
make restore    # Load initial dataset
make dev        # Start development server (port 5001)
```

## Critical Requirement: Use `uv`

All Python operations must use `uv` as the package and task manager. Direct use of `pip` or `python` is prohibited.

```bash
uv run python <script>
uv run pytest
uv run alembic <command>
```

## Development Commands

### Application
- `make dev` - Development server with hot reload
- `make run` - Production-like server via Gunicorn

### Code Quality
- `make format` - Format with Black (120 character lines)
- `make lint` - Check with Ruff
- `make lint-fix` - Auto-fix linting issues

### Testing
- `make test` - All tests with coverage
- `make test-unit` - Unit tests only
- `make test-integration` - Integration tests

**Testing Targets**: < 4 seconds execution, < 100 tests, > 50% coverage on critical business logic.

### Database
- `make db-setup` - Apply migrations
- `make db-init` - Fresh database
- `make db-reset` - Reset (development only)
- `make backup` / `make restore` - Backup/restore

## Testing Strategy

The test suite prioritises high-value regression protection:

**Tested**: User-facing features, error paths, business logic  
**Not Tested**: Third-party libraries, UI rendering, implementation details  
**Tools**: pytest, SQLAlchemy (in-memory SQLite for integration tests)

**Running Tests**:
```bash
make test                    # All tests with coverage
make test-unit               # Fast unit tests only
uv run pytest tests/unit/    # Specific directory
```

## Code Quality

- **Black**: 120-character line formatting
- **Ruff**: Fast Python linter combining multiple tools
- **Type Hints**: Required for all functions using modern syntax (`list[str]`)

**Pre-commit Workflow**:
```bash
make format && make lint && make test
```

## Database Management

Alembic manages version-controlled schema migrations:

```bash
uv run alembic revision --autogenerate -m "Description"  # Create migration
uv run alembic upgrade head                               # Apply migrations
uv run alembic downgrade -1                               # Revert last
uv run alembic current                                    # Check status
```

**Workflow**:
```bash
# Development
make db-setup     # Apply migrations
make db-init      # Fresh database

# Production
make backup       # Create backup
make db-setup     # Apply migrations
```

## Production Deployment

### Containerisation

Multi-stage Docker build:
- **Build Stage**: Uses `uv` to install dependencies from `pyproject.toml`
- **Production Stage**: Minimal Python slim image with Gunicorn as WSGI server
- **Non-root User**: Application runs as non-root for security

Health check endpoint: `/api/hello`

### Container Orchestration

Docker Compose manages the application:

```bash
# Development
docker compose up --build -d

# Production (pre-built images from GitHub Container Registry)
docker compose -f compose.prod.yml up -d

# View logs
docker compose logs -f server
```

### Configuration

Pydantic Settings maps environment variables to strongly-typed configuration:

**Required Variables**:
- `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL_NAME` - Ollama LLM service
- `FLASK_SECRET_KEY`, `JWT_SECRET_KEY` - Security keys
- `SQLALCHEMY_DATABASE_URI` - PostgreSQL connection
- `SMTP_*` - Email service configuration
- `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` - Bootstrap admin

The `ENVIRONMENT` variable enables runtime behavior adaptation without code changes.

## Related Documentation

- [Server Architecture](server-architecture.md) - Architectural decisions and design patterns
- [API Documentation](api.md) - REST API endpoints and patterns
- [Client Deployment](../client/client-cicd.md) - Client deployment procedures
