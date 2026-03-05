.PHONY: build run clean test help setup dev format lint

# Variables
MVN := ./mvnw
PROJECT := learn-hub-server
# Java 21 is required. Override on the command line if needed: make JAVA_HOME=/path/to/java21
JAVA_HOME := /usr/lib/jvm/temurin-21-jdk-amd64
export JAVA_HOME
export PATH := $(JAVA_HOME)/bin:$(PATH)

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
setup: ## Download dependencies and compile the project
	$(MVN) dependency:resolve compile -q

# Development
dev: ## Run Spring Boot application in development mode
	$(MVN) spring-boot:run

run: ## Run the application
	$(MVN) spring-boot:run

# Build
build: ## Build the application
	$(MVN) clean package -DskipTests

build-docker: ## Build Docker image
	docker build -t $(PROJECT) .

# Testing
test: ## Run all tests
	$(MVN) test

test-integration: ## Run integration tests
	$(MVN) verify -P integration-tests

# Code quality
format: ## Format code with Spotless
	$(MVN) spotless:apply

lint: ## Check code style with Spotless
	$(MVN) spotless:check

# Database operations
db-migrate: ## Run Flyway migrations
	$(MVN) flyway:migrate

db-clean: ## Clean database
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
