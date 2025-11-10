# syntax=docker/dockerfile:1
# Multi-stage build for optimized production image
# Target both arm64 and amd64 architectures

# Build stage with uv
FROM ghcr.io/astral-sh/uv:python3.13-bookworm-slim AS builder

# Set working directory
WORKDIR /app

# Copy dependency files
COPY pyproject.toml uv.lock ./

# Install dependencies with cache mount for faster builds
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev

# Copy application code
COPY app/ ./app/
COPY scripts/ ./scripts/
COPY app/db/migrations/versions/ ./migrations/versions/

# Production stage with Python slim image
FROM python:3.13-slim-bookworm AS production

# Add labels for better image management
LABEL org.opencontainers.image.title="LEARN-Hub Server"
LABEL org.opencontainers.image.description="Flask API server for LEARN-Hub application"
LABEL org.opencontainers.image.vendor="LEARN-Hub Project"

# Set environment variables
ENV PYTHONPATH=/app
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV UV_LINK_MODE=copy

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy virtual environment from builder stage
COPY --from=builder /app/.venv /app/.venv

# Copy application code
COPY --from=builder /app/app ./app
COPY --from=builder /app/migrations ./migrations
COPY --from=builder /app/scripts ./scripts

# Expose application port
EXPOSE 5001

# Health check using curl
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD ["curl", "-f", "http://localhost:5001/hello"] || exit 1

# Run the application with Gunicorn
ENTRYPOINT ["/app/.venv/bin/gunicorn"]
CMD ["--bind", "0.0.0.0:5001", "--workers", "4", "--worker-class", "sync", "--timeout", "120", "--keep-alive", "2", "--max-requests", "1000", "--max-requests-jitter", "100", "app.main:get_app()"]