# LEARN-Hub

A prototype recommendation system for Computer Science education activities that uses automated content processing and scoring algorithms.

## Quick Start

```bash
# Setup
make setup
make db-setup
make restore  # Load initial data

# Run locally
make dev      # Starts server on 5001 and client on 3001

# Or with Docker
docker compose up --build -d
```

## Services

- **Client**: http://localhost:3001
- **Backend API**: http://localhost:5001
- **API Docs**: http://localhost:5001/api/apidocs

## Environment

Copy and configure environment variables:
```bash
cp example.env .env
```

Key variables:
- `GOOGLE_API_KEY` - For automated PDF processing
- `DEV_SECRET_KEY` - Development authentication
- `FLASK_SECRET_KEY` - Flask session security

## Documentation

All documentation is organized in the `docs/` directory with clear separation by concern:

### Core System
- [Recommendation Logic](docs/core/logic.md) - Core algorithms and scoring system
- [Implementation Details](docs/core/implementation.md) - System implementation and architecture

### Server (Backend)
- [Architecture](docs/server/architecture.md) - Server architecture and design patterns
- [API Documentation](docs/server/api.md) - REST API endpoints and integration
- [CI/CD Guide](docs/server/cicd.md) - Deployment and operational procedures

### Client (Frontend)
- [Client Documentation](docs/client/) - Frontend architecture, components, and deployment *(Work in Progress)*

## Recent Improvements

### Frontend Architecture
The frontend features a clean, maintainable architecture with:

- **✅ Staff Engineer Refactoring**: 86% reduction in ActivityCreationModal complexity (617→84 lines)
- **✅ Context Consolidation**: Eliminated 4 duplicate context files, streamlined architecture
- **✅ Component Separation**: Extracted reusable ActivityForm component for better maintainability
- **✅ Type Safety**: Replaced `any` types with proper TypeScript interfaces
- **✅ Shared Components**: `ActivityCard` component eliminates code duplication
- **✅ Modern Stack**: React 19, TypeScript, Vite, Tailwind CSS
- **✅ Dark Mode**: Comprehensive theme system with automatic detection and persistence
- **✅ Improved Colors**: Enhanced color palette with better contrast and accessibility
- **✅ Integrated Forms**: Recommendation forms embedded directly in dashboards

### Backend API
Streamlined REST API with:

- **✅ Dual Authentication**: Admin (email/password) + Teacher (verification codes)
- **✅ Automated Processing**: Google Gemini for PDF processing and content analysis
- **✅ User History**: Search history tracking and favorites management
- **✅ Comprehensive Testing**: 157 tests with 78% coverage
- **✅ Clean Documentation**: Concise Swagger/OpenAPI documentation
- **✅ Standardized Responses**: Consistent API response format with proper client handling

## Tech Stack

- **Backend**: Python 3.13, Flask, SQLAlchemy, SQLite
- **Client**: React 19, TypeScript, Vite, Tailwind CSS
- **Automated Processing**: Google Gemini 2.5 Flash, LangChain
- **Testing**: pytest, coverage reporting
- **Deployment**: Docker, Docker Compose