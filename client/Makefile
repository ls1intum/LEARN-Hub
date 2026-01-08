# LEARN-Hub Client Makefile for React + Vite + shadcn/ui

.PHONY: help setup dev build preview lint format test test-ui clean docker-build docker-run

# Default target
help:
	@echo "Available commands:"
	@echo "  setup     - Install dependencies"
	@echo "  dev         - Start development server"
	@echo "  build       - Build for production"
	@echo "  lint        - Run ESLint"
	@echo "  format      - Format code with Prettier"
	@echo "  test        - Run tests"
	@echo "  test-ui     - Run tests with UI"
	@echo "  clean       - Clean build artifacts"
	@echo "  docker-build-local - Build Docker image (current platform)"
	@echo "  docker-run  - Run Docker container"

# Install dependencies
setup:
	npm install

# Start development server
dev:
	@if [ -f ../.env ]; then \
		export $$(grep -v '^#' ../.env | xargs) && \
		CLIENT_PORT=$${CLIENT_PORT:-3001} npm run dev; \
	else \
		CLIENT_PORT=$${CLIENT_PORT:-3001} npm run dev; \
	fi

# Build for production
build:
	npm run build

# Run linter with fix
lint:
	npm run lint

# Format code
format:
	npm run format

# Run tests
test:
	npm run test:run

# Run tests with UI
test-ui:
	npm run test:ui

# Clean build artifacts
clean:
	rm -rf dist
	rm -rf node_modules/.vite

# Build Docker image for current platform
docker-build-local:
	docker build -t learn-hub-client .

# Run Docker container
docker-run:
	docker run -p 3001:3001 learn-hub-client
