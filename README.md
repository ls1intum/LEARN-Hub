# LEARN-Hub

A recommendation system prototype for Computer Science education activities that leverages automated content processing and category-based scoring algorithms to support teachers in activity selection and lesson planning.

## Overview

LEARN-Hub addresses the challenge of finding appropriate educational activities for computer science courses by implementing an intelligent recommendation engine. The system processes educational activity documents, analyzes their pedagogical characteristics, and generates personalized recommendations based on teacher requirements such as target age group, available resources, and learning objectives aligned with Bloom's taxonomy.

## Architectural Overview

The system implements a three-tier architecture designed for scalability and maintainability:

**Client Layer**: A React single-page application (SPA) provides an interactive user interface for teachers and administrators. The client implements role-based access control, supports both light and dark themes, and maintains session-based authentication for enhanced security.

**Server Layer**: A Flask REST API server handles all business logic, including the core recommendation engine, user management, and content processing. The API uses automated processing to extract structured data from PDF documents and generate activity recommendations using a category-based scoring system.

**Data Layer**: PostgreSQL serves as the primary data store, managing activities, user accounts, search history, and favorites. The database schema supports complex relationships between activities, topics, and user preferences while maintaining referential integrity.

**Containerization**: Docker and Docker Compose orchestrate the deployment of all services, ensuring consistent environments across development, staging, and production deployments.

## Quick Start

```bash
# Setup environment and dependencies
make setup
make db-setup
make restore  # Load initial data

# Run locally (development mode)
make dev      # Starts server on port 5001 and client on port 3001

# Or run with Docker
docker compose up --build -d
```

## Services

Once running, access the system at:

- **Client**: http://localhost:3001
- **Server API**: http://localhost:5001
- **API Documentation**: http://localhost:5001/api/openapi

## Environment Configuration

The application requires environment variables for API integrations, security keys, and database configuration:

```bash
cp example.env .env
```

Key configuration variables:
- `LLM_API_KEY` - API key for automated content processing
- `FLASK_SECRET_KEY` - Flask session security
- `JWT_SECRET_KEY` - JWT token signing
- `SQLALCHEMY_DATABASE_URI` - PostgreSQL connection string
- `SMTP_*` - Email service configuration for teacher verification

See `example.env` for a complete list of configurable variables.

## Documentation

**User Documentation**: https://ls1intum.github.io/LEARN-Hub/

**Developer Documentation** is organized in the `docs/` directory by architectural layer:

#### Core System
- [`docs/core/recommendation-engine.md`](docs/core/recommendation-engine.md) - Recommendation algorithm design, scoring methodology, and architectural decisions

### Server
- [`docs/server/server-architecture.md`](docs/server/server-architecture.md) - Server architecture and design patterns
- [`docs/server/api.md`](docs/server/api.md) - REST API endpoints and data models
- [`docs/server/server-cicd.md`](docs/server/server-cicd.md) - Development workflow and deployment procedures

### Client
- [`docs/client/client-architecture.md`](docs/client/client-architecture.md) - Client architecture and component design
- [`docs/client/api-integration.md`](docs/client/api-integration.md) - Client-server integration patterns
- [`docs/client/client-cicd.md`](docs/client/client-cicd.md) - Build system and deployment

## Technology Stack

**Server**:
- Python 3.13 with modern language features
- Flask 3.0 for REST API implementation
- SQLAlchemy ORM with Alembic migrations
- PostgreSQL 15+ for data persistence
- Gunicorn for production deployment

**Client**:
- React 19 with TypeScript
- Vite for build tooling
- Tailwind CSS for styling
- Nginx for production serving

**Infrastructure**:
- Docker for containerization
- Docker Compose for orchestration
- GitHub Container Registry for image distribution

**Development Tools**:
- `uv` for Python dependency management
- pytest for server testing
- Vitest for client testing
- ESLint and Ruff for code quality

## Development Commands

### Server
```bash
cd server/
make dev          # Start development server
make test         # Run tests with coverage
make lint         # Check code quality
make format       # Format code
make db-setup     # Run database migrations
```

### Client
```bash
cd client/
make dev          # Start development server
make test         # Run tests
make build        # Build for production
make lint         # Check code quality
```

