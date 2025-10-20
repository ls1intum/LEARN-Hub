# Server Architecture

## Overview

Flask-based REST API server providing activity recommendations with scoring algorithms, PDF processing, user management, and lesson plan generation with PostgreSQL persistence.

## System Architecture

### Core Components

**Application Layer** (`app/`):
- **API Routes** (`api/`): RESTful endpoints with Flask-OpenAPI3 for automatic OpenAPI 3.0 generation
- **Authentication** (`auth/`): JWT-based dual authentication system
- **Services** (`services/`): Business logic and external integrations
- **Database Models** (`db/models/`): SQLAlchemy ORM models
- **Core Engine** (`core/`): Recommendation system and automated processing

**Infrastructure Layer**:
- **PostgreSQL**: Primary data persistence
- **File Storage**: PDF document storage and processing
- **LLM Integration**: Activity recommendations and lesson plan generation
- **Email Service**: Teacher verification codes

### Technology Stack

**Backend Framework**: Flask 3.0 with Python 3.13 features
**Database**: PostgreSQL 15+ with SQLAlchemy ORM and Alembic migrations
**Automated Processing**: LLM integration for recommendations and content generation
**Authentication**: JWT tokens with role-based access control
**Validation**: Pydantic v2 for type-safe request/response handling
**Documentation**: Flask-OpenAPI3 with automatic OpenAPI 3.0 specification generation

## Authentication Architecture

### Dual Authentication Model

**Admin Access** (Content Management):
- Email/password authentication
- Full CRUD operations on activities and users
- PDF upload and activity creation capabilities

**Teacher Access** (Activity Discovery):
- Email verification code system OR password authentication
- Read-only access to activity library
- Lesson plan generation and favorites management

### Security Implementation

**JWT Token Management**:
- Access tokens (15-minute expiration)
- Refresh tokens (30-day expiration)
- Automatic token refresh in client applications

**Request Validation**:
- Server-side validation using Pydantic models
- No trust of client data - all inputs validated
- Input sanitization and type checking

## Database Architecture

### Data Models

**Core Entities**:
- `User`: Admin and teacher accounts with role-based permissions
- `Activity`: Educational activities with mandatory PDF document references and database-level validation
- `UserSearchHistory`: Teacher search queries for recommendation tracking
- `UserFavourites`: User favourites supporting both individual activities and lesson plans with simplified data structure

**Relationships**:
- One-to-many: User → SearchHistory, User → Favourites
- One-to-one: Activity → PDF Document (via document_id reference)
- Many-to-many: Activity ↔ Topics (through association table)

### Data Model Architecture

**Unified Data Flow**:
- **Database**: `Activity` (SQLAlchemy ORM model)
- **API Layer**: `ActivityModel` (Pydantic model for validation and serialization)
- **Core Engine**: `ActivityModel` (Pydantic model for recommendations)

**Key Benefits**:
- **Type Safety**: Pydantic models provide runtime validation and type hints
- **Single Source of Truth**: ActivityModel serves both API and core engine
- **Performance**: Direct conversion from database to Pydantic models
- **Maintainability**: Consistent data structures across all layers

### Migration Strategy

**Alembic Integration**:
- Version-controlled database schema changes
- Automatic migration generation from model changes
- Production-safe migration rollback capabilities

## Service Architecture

### Service Layer Pattern

**PDF Services**:
- `PDFService`: Unified PDF storage, retrieval, and lesson plan generation

**Business Services**:
- `RecommendationService`: Activity recommendations with scoring algorithms using ActivityModel
- `UserService`: User management and authentication
- `EmailService`: Verification code delivery
- `UserSearchHistoryService`: User preference tracking and search history management
- `UserFavouritesService`: User favourites management for both individual activities and lesson plans with CRUD operations and status checking

**Integration Services**:
- `LLMClient`: External LLM API integration for content generation

### Dependency Injection

**Service Initialization**:
- Direct service instantiation with dependency injection
- Testable service dependencies through constructor injection
- Configuration-driven service initialization

## API Architecture

### Route Organization

**Flask-OpenAPI3 Architecture**:
- **Modular Route Registration**: Each API module registers routes with Flask-OpenAPI3
- **Automatic OpenAPI Generation**: OpenAPI 3.0 specification generated automatically
- **Type-Safe Validation**: Pydantic models provide request/response validation
- **Response Format**: All endpoints return standardized JSON responses

**Route Implementation**:
- `app.api.activities.listing` - Activity discovery and management
- `app.api.activities.recommendations` - Recommendation engine endpoints
- `app.api.activities.lesson_plan` - Lesson plan generation
- `app.api.activities.creation` - Activity creation (admin only)
- `app.api.activities.pdf` - PDF processing for activities
- `app.api.history` - User search history and favourites management
- `app.api.auth` - Authentication and user management
- `app.api.documents` - PDF upload and processing
- `app.api.meta` - System metadata and field values

### RESTful Design

**Resource-Based URLs**:
- `/api/activities/` - Activity discovery, recommendations, and management
- `/api/history/` - User search history and favourites management
- `/api/auth/` - Authentication and user management
- `/api/documents/` - PDF upload and processing
- `/api/meta/` - System metadata and field values

**HTTP Methods**:
- GET: Resource retrieval with filtering and pagination
- POST: Resource creation and complex operations (recommendations, lesson plans)
- PUT: Resource modification
- DELETE: Resource removal

### Response Standardization

**Direct Response Format**:
```json
// Success responses return data directly
{ /* response data */ }

// Error responses use consistent error format
{
  "error": "Error message describing what went wrong"
}
```

**Error Handling**:
- Standardized error codes and messages
- Validation error responses
- Proper HTTP status codes

## Configuration Management

### Environment-Based Configuration

**Pydantic Settings** (`app/utils/config.py`):
- Type-safe configuration validation
- Environment variable mapping
- Default value management
- Production vs. development settings

**Configuration Categories**:
- Database connection settings
- LLM service API keys
- Email service configuration
- Security settings (JWT secrets, CORS)

## File Storage Architecture

### PDF Document Management

**Storage Strategy**:
- Filesystem-only storage with configurable paths
- Document ID-based file naming: `doc_{id}_{filename}` for easy reconstruction
- No database table for PDF metadata
- Test fallback for missing documents during development

**Processing Pipeline**:
1. PDF upload → validation → filesystem storage → return document_id
2. Activity creation → validate document_id exists → create activity
3. PDF retrieval → lookup by document_id → return file content
4. Lesson plan generation → summary page + PDF merging

**Service Consolidation**:
- Single `PDFService` handles all PDF operations
- Unified interface for storage, retrieval, and lesson plan generation
- API endpoints with consistent error handling

## External Integrations

### LLM Integration

**API Usage**:
- Activity recommendation generation
- Lesson plan content creation
- Error handling and rate limiting
- API key management and rotation

### Email Service Integration

**SMTP Configuration**:
- Development: Local SMTP server (MailHog)
- Production: External SMTP provider
- Template-based email generation
- Delivery tracking and error handling

## Performance Optimizations

**Recommendation Engine**:
- Early filtering: Zero-score activities removed before expensive operations
- Series generation limits: Top 20 activities only, reducing complexity from O(n^k) to O(20^k)
- Optimized for 50-activity dataset with responsive user experience
- Memory-efficient data structures using Pydantic models

### Health Checks

**Endpoint Monitoring**:
- `/hello` - Basic application health
- `/openapi` - API documentation and health check

### Logging Strategy

**Structured Logging**:
- Request/response logging
- Error tracking and alerting
- Performance metrics collection
- Security event logging

## Deployment Architecture

### Container Strategy

**Docker Integration**:
- Multi-stage builds for optimization
- Environment-specific configurations
- Health check integration
- Resource limit management
