.PHONY: setup format lint lint-fix test test-verbose clean help dev run backup restore db-setup requirements build-docker run-docker

# Variables
PYTHON := uv run python
PIP := uv run pip
RUFF := uv run ruff
BLACK := uv run black
PROJECT := app
TESTS := tests
PYTEST := uv run pytest
MAIN := ${PROJECT}/main.py
SCRIPTS := scripts
ALEMBIC := uv run alembic -c alembic.ini

# Default target
.DEFAULT_GOAL := help

# Help command
help: ## Show server-specific commands
	@echo "LEARN-Hub Server (Python/Flask) Commands"
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

# Setup and dependencies
setup: ## Install Python dependencies and set up virtual environment
	uv sync

# Development
dev: ## Run Flask server in development mode
	FLASK_APP=$(PROJECT).main:get_app FLASK_ENV=development $(PYTHON) -m flask run --host=0.0.0.0 --port=5001 --debug

run: ## Run the main application
	$(PYTHON) $(MAIN)

# Code quality
format: ## Format code with Black
	$(BLACK) $(PROJECT) $(TESTS) $(MAIN) $(SCRIPTS)

lint: ## Lint code with Ruff
	$(RUFF) check $(PROJECT) $(TESTS) $(MAIN) $(SCRIPTS)

lint-fix: ## Lint and auto-fix with Ruff
	$(RUFF) check --fix $(PROJECT) $(TESTS) $(MAIN) $(SCRIPTS)

# Testing
test: ## Run all tests in parallel (optimized startup)
	$(PYTEST) -n 4 --maxfail=1 --tb=short $(TESTS)

test-unit: ## Run unit tests in parallel (optimized startup)
	$(PYTEST) -n 4 --maxfail=1 --tb=short $(TESTS)/unit

test-integration: ## Run integration tests in parallel (optimized startup)
	$(PYTEST) -n 4 --maxfail=1 --tb=short $(TESTS)/integration

test-coverage: ## Run all tests with coverage (single-threaded)
	$(PYTEST) --cov=$(PROJECT) --cov-report=term-missing $(TESTS)

# Database operations
db-setup: ## Apply all Alembic migrations
	$(ALEMBIC) upgrade head

db-migrate: ## Run database migrations (alias for db-setup)
	$(ALEMBIC) upgrade head

db-check: ## Check that migrations are up to date
	@$(ALEMBIC) check || (echo "ERROR: Migrations not up to date. Run 'make db-setup' first." && exit 1)

db-mock: db-check ## Populate database with mock data (requires migrations applied)
	$(PYTHON) scripts/setup_db.py mock

db-dataset: db-check ## Import real dataset (requires migrations applied)
	$(PYTHON) scripts/setup_db.py dataset --csv ../dataset/dataset.csv --pdf-dir ../dataset/pdfs

db-reset: ## Reset database (delete and recreate)
	$(ALEMBIC) downgrade base
	$(ALEMBIC) upgrade head

# Docker operations
build-docker: ## Build the Docker image (multi-arch)
	docker buildx build --platform linux/amd64,linux/arm64 -t learn-hub-server .

build-docker-local: ## Build the Docker image (current platform)
	docker build -t learn-hub-server .

run-docker: build-docker-local ## Run the Docker container
	docker run -p 5001:5001 \
		--env-file ../.env \
		learn-hub-server

# Cleanup
clean: ## Remove build artifacts
	rm -rf build/
	rm -rf dist/
	rm -rf *.egg-info
	find . -type d -name __pycache__ -exec rm -rf {} +
	find . -type d -name .pytest_cache -exec rm -rf {} +
	find . -type d -name .coverage -exec rm -rf {} +
	find . -type f -name "*.pyc" -delete

# Utility
version: ## Show installed package versions
	uv lock --upgrade

import-dataset-prod: ## DEPRECATED: Use 'make db-dataset' from host instead
	@echo "WARNING: This command runs inside Docker container. Recommended: Run 'make db-dataset' from host."
	docker exec lhub-server /app/.venv/bin/python scripts/setup_db.py dataset --csv /app/dataset/dataset.csv --pdf-dir /app/dataset/pdfs
