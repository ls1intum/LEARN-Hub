.PHONY: help setup dev dev-server dev-client docker-up docker-down docker-build clean format lint test install

# Default target
.DEFAULT_GOAL := help

# Help command
help: ## Show this help
	@echo "LEARN-Hub - Top Level Commands"
	@echo "Usage: make [target]"
	@echo ""
	@echo "Development:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E "(dev|install|setup)" | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "Docker:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep "docker" | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "Code Quality:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E "(format|lint|test|clean)" | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "Database:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep -E "(db-setup|db-migrate|db-check|db-mock|db-dataset|db-reset|backup|restore)" | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "Server-specific commands: cd server && make help"
	@echo "Client-specific commands: cd client && make help"

# Setup targets
setup: server-setup client-setup ## Install all dependencies for server and client

server-setup: ## Install server dependencies
	cd server && $(MAKE) setup

client-setup: ## Install client dependencies  
	cd client && $(MAKE) setup

# Development targets
dev: ## Run both server and client locally for development
	@echo "Starting local development..."
	@echo "Server will be at http://localhost:5001"
	@echo "Client will be at http://localhost:3001"
	$(MAKE) -j2 dev-server dev-client

dev-server: ## Run Flask server locally
	cd server && $(MAKE) dev

dev-client: ## Run client server locally
	cd client && $(MAKE) dev

# Docker targets
docker-up: ## Start services with Docker Compose
	docker compose up --build -d

docker-build: ## Build Docker images
	docker compose build

docker-prod: ## Start production services
	docker compose -f compose.prod.yml up -d

# Code quality targets (run on both server and client)
format: ## Format code in both server and client
	cd server && $(MAKE) format
	cd client && $(MAKE) format

lint: ## Lint code in both server and client
	cd server && $(MAKE) lint
	cd client && $(MAKE) lint

test: ## Run tests for server
	cd server && $(MAKE) test
	cd client && $(MAKE) test

clean: ## Clean build artifacts for both server and client
	cd server && $(MAKE) clean
	cd client && $(MAKE) clean || true

# Database targets
db-setup: ## Apply all Alembic migrations
	cd server && $(MAKE) db-setup

db-check: ## Check that migrations are up to date
	cd server && $(MAKE) db-check

db-mock: ## Populate database with mock data (requires migrations applied)
	cd server && $(MAKE) db-mock

db-dataset: ## Import real dataset (requires migrations applied)
	cd server && $(MAKE) db-dataset

db-reset: ## Reset database (delete and recreate)
	cd server && $(MAKE) db-reset
