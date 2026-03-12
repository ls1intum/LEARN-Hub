.PHONY: build run clean test help setup

# Variables
MVN := ./mvnw
PROJECT := learn-hub-server

# Default target
.DEFAULT_GOAL := help

# Help command
help: ## Show server-specific commands
@echo "LEARN-Hub Server (Java/Spring Boot) Commands"
@echo "Usage: make [target]"
@echo ""
@echo "Targets:"
@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

# Setup
setup: ## Resolve and download all Maven dependencies
$(MVN) dependency:resolve -q

# Development
dev: ## Run Spring Boot application in development mode
$(MVN) spring-boot:run

run: ## Run the application
$(MVN) spring-boot:run

# Build
build: ## Build the application JAR
$(MVN) clean package -DskipTests

build-docker: ## Build Docker image
docker build -t $(PROJECT) .

# Testing
test: ## Run all tests
$(MVN) test

# Code quality
format: ## Format code with Spotless
$(MVN) spotless:apply

lint: ## Check code style with Spotless
$(MVN) spotless:check

# Database operations
db-migrate: ## Run Flyway migrations
$(MVN) flyway:migrate

db-clean: ## Clean database (WARNING: deletes all data)
$(MVN) flyway:clean

# Docker operations
run-docker: build-docker ## Build and run Docker container
docker run -p 5001:5001 \
--env-file ../.env \
$(PROJECT)

# Cleanup
clean: ## Remove build artifacts
$(MVN) clean
rm -rf target/

# Utility
version: ## Show version information
$(MVN) --version
java -version
