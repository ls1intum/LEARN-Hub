.PHONY: help setup dev dev-server dev-client docker-up docker-build docker-prod format lint test clean db-migrate db-clean

# Default target
.DEFAULT_GOAL := help

# Help command
help: ## Show this help
@echo "LEARN-Hub - Top Level Commands"
@echo "Usage: make [target]"
@echo ""
@echo "Development:"
@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E "(dev|setup)" | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
@echo ""
@echo "Docker:"
@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep "docker" | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
@echo ""
@echo "Code Quality:"
@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E "(format|lint|test|clean)" | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
@echo ""
@echo "Database:"
@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E "(db-migrate|db-clean)" | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
@echo ""
@echo "Server-specific commands: cd server && make help"
@echo "Client-specific commands: cd client && make help"

# Setup targets
setup: ## Install all dependencies for server and client
cd server && $(MAKE) setup
cd client && npm ci

# Development targets
dev: ## Run both server and client locally for development
@echo "Starting local development..."
@echo "Server will be at http://localhost:5001"
@echo "Client will be at http://localhost:3001"
$(MAKE) -j2 dev-server dev-client

dev-server: ## Run server locally
cd server && $(MAKE) dev

dev-client: ## Run client locally
cd client && npm run dev

# Docker targets
docker-up: ## Start all services with Docker Compose
docker compose -f docker/compose.yml up --build -d

docker-build: ## Build Docker images
docker compose -f docker/compose.yml build

docker-prod: ## Start production services with pre-built images
docker compose -f docker/compose.prod.yml up -d

# Code quality
format: ## Format code in server and client
cd server && $(MAKE) format
cd client && npm run format || true

lint: ## Lint code in server and client
cd server && $(MAKE) lint
cd client && npm run lint

test: ## Run tests for server and client
cd server && $(MAKE) test
cd client && npm run test:run

clean: ## Clean build artifacts
cd server && $(MAKE) clean
cd client && rm -rf dist node_modules || true

# Database targets
db-migrate: ## Run Flyway database migrations
cd server && $(MAKE) db-migrate

db-clean: ## Clean database (WARNING: deletes all data)
cd server && $(MAKE) db-clean
