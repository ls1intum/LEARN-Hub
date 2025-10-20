# CI/CD and Operations

## Overview

Deployment and operational procedures for the Flask backend. This document provides high-level guidance for development, deployment, and maintenance procedures.

## Development Environment

### Quick Start

```bash
cd server/
make setup      # Install dependencies with uv
make db-setup   # Run database migrations
make restore    # Load initial data
make dev        # Start development server
```

### Environment Configuration

Copy `example.env` to `.env` and configure required variables:
- LLM Integration: `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL_NAME`
- Security: `FLASK_SECRET_KEY`, `JWT_SECRET_KEY`
- Database: PostgreSQL connection settings
- Email: SMTP configuration
- Admin: Initial admin account setup

See `example.env` for complete variable list and `server/Makefile` for all available commands.

#### SMTP Variables
The Docker Compose service expects these SMTP environment variables:

- `SMTP_SERVER`
- `SMTP_PORT`
- `EMAIL_USERNAME`
- `EMAIL_PASSWORD`
- `EMAIL_ADDRESS`

The application reads these via `compose.yml`.

### Development Commands

**Code Quality**: `make format`, `make lint`, `make lint-fix`
**Testing**: `make test`, `make test-unit`, `make test-integration`
**Database**: `make db-setup`, `make db-init`, `make db-reset`, `make backup`, `make restore`

### Testing Strategy

**Test Suite**: Test coverage with fast execution
- **Unit Tests**: Core business logic, models, validation, and services
- **Integration Tests**: API endpoints, database operations, error handling

**Focus**: Critical business functionality and error paths only
**Commands**: `make test` (all), `make test-unit` (fast), `make test-integration`

Refer to `server/Makefile` for complete command reference.

## Production Deployment

### Docker Deployment

**Local Setup**:
```bash
docker compose up --build -d
docker compose logs -f server
```

**Production Configuration**:
- Use `server/Dockerfile` for container builds
- Configure production environment variables
- Set up health checks and resource limits
- Use `compose.yml` for orchestration

### Production Considerations

**Database**: PostgreSQL with connection pooling, read replicas, automated backups
**Security**: HTTPS termination, strong secrets, input validation, rate limiting
**Performance**: Multiple workers, load balancing, caching, resource monitoring

See `server/Dockerfile` and `compose.yml` for implementation details.

## Database Management

### Migration Management

**Alembic Commands**:
- Create migration: `uv run alembic revision --autogenerate -m "Description"`
- Apply migrations: `uv run alembic upgrade head`
- Rollback: `uv run alembic downgrade -1`
- Check status: `uv run alembic current`

**Note**: All Alembic commands must be run with `uv run` prefix as per project standards.

### Database Operations

**Make Commands**:
- `make db-setup` - Run migrations
- `make db-init` - Initialize fresh database
- `make db-reset` - Reset database (development only)
- `make backup` - Create data backup
- `make restore` - Restore from backup

See `server/Makefile` for complete database command reference.

## Monitoring and Health Checks

### Health Check Endpoints

- `/hello` - Basic application health
- `/openapi` - API documentation and health check

### Logging and Monitoring

**Application Logs**: `docker compose logs -f server`
**Database Logs**: `docker compose logs -f db`
**Performance**: Monitor request times, database performance, resource usage, error rates

## Troubleshooting

### Common Issues

**Database**: Check connectivity with `docker compose exec server psql -h db -U postgres -d cs_activities`, reset with `make db-reset`
**API Keys**: Verify with `docker compose exec server env | grep LLM_API_KEY`
**API Documentation**: Check `/openapi` endpoint for interactive documentation
**Port Conflicts**: Check with `lsof -i :5001`, kill conflicting processes
**Python Environment**: Ensure using `uv` for all Python operations, not direct `python` commands

### Error Diagnosis

**Application**: Check logs, verify environment variables, test database connectivity
**Database**: Check PostgreSQL logs, verify migration status, check disk space
**Performance**: Monitor resource usage, check query performance, scale as needed

## Security and Maintenance

### Security Procedures

**Environment**: Set proper permissions with `chmod 600 .env`
**API Keys**: Rotate regularly, use environment variables, monitor usage
**Database**: Use SSL in production, implement connection pooling

### Backup and Recovery

**Backup**: Automated daily database backups, PDF and config backups, offsite storage
**Recovery**: Database with `make restore`, application with `git pull && docker compose up --build -d`

### Maintenance

**Regular Tasks**: Weekly log review, monthly security reviews, quarterly audits
**Application Updates**: `git pull && make db-setup && docker compose up --build -d`
**Scaling**: Multiple instances, read replicas, connection pooling, resource monitoring
**Code Quality**: Run `make lint` and `make format` before committing changes

## Related Documentation

- [Server Architecture](server-architecture.md) - System architecture and design patterns
- [API Documentation](api.md) - REST API endpoints and integration
- [Client Deployment](../client/client-cicd.md) - Frontend deployment procedures
