# API Documentation

## Overview

RESTful API providing activity recommendations with transparent scoring, lesson plan generation, and user preference tracking with dual authentication for admins and teachers.

**Base URL**: `http://localhost:5001` (development) | `https://your-domain.com` (production)  
**Documentation**: `/api/openapi` (Swagger UI) | `/openapi.json` (OpenAPI spec) | [Hosted Swagger UI (test deployment)](https://learnhub-test.aet.cit.tum.de/api/openapi/swagger)

## Authentication

### Dual Authentication System

**Admin Access**: Email/password authentication with JWT tokens for full system access (activity creation, user management, PDF upload).

**Teacher Access**: Email verification codes (6-digit, 10-minute expiry, 3-attempt limit) OR password authentication for activity discovery, lesson planning, and favourites management.

### Core Endpoints

**Authentication**: `POST /api/auth/admin/login`, `POST /api/auth/login`, `POST /api/auth/verification-code`, `POST /api/auth/verify`, `POST /api/auth/refresh`, `GET /api/auth/me`, `PUT /api/auth/me`, `DELETE /api/auth/me`

## API Architecture

### Flask-OpenAPI3 Integration

- Automatic OpenAPI 3.0 specification generation from Pydantic models
- Type-safe request/response validation
- Automatic Swagger UI at `/api/openapi`
- JWT Bearer authentication with automatic token validation
- Comprehensive error handling

### Request Validation

Pydantic v2 models enforce type safety and validation: email format validation, range constraints (age 6-15, duration 1-300 minutes), enum validation for formats/bloom levels/energy levels, automatic list conversion for consistency.

## Core API Patterns

### Resource-Based Design

Resources organised by domain:
- `/api/activities/` - Activity discovery and management
- `/api/auth/` - Authentication and user management
- `/api/history/` - User history and favourites
- `/api/documents/` - PDF upload and retrieval
- `/api/meta/` - System metadata and configuration

### Recommendation Endpoints

**GET /api/activities/recommendations**: Accepts teacher-specified criteria (target age, format, Bloom levels, duration, topics, priority categories), returns ranked recommendations with detailed scoring breakdowns.

**POST /api/activities/lesson-plan**: Generates multi-activity lesson plans from activities list with optional breaks. Break constraints enforce that breaks appear only between activities (not after the final activity).

**GET /api/activities/scoring-insights**: Returns information about scoring categories and their impact weights.

## User Management

### Teacher-Specific Endpoints

**Search History** (authenticated teachers):
- `GET /api/history/search` - Retrieve previous searches with pagination
- `DELETE /api/history/search/{id}` - Remove search history entry

**Favourites** (authenticated teachers):
- `POST /api/history/favourites/activities`, `GET /api/history/favourites/activities`, `DELETE /api/history/favourites/activities/{id}` - Individual activity favourites
- `POST /api/history/favourites/lesson-plans`, `GET /api/history/favourites/lesson-plans`, `DELETE /api/history/favourites/{id}` - Lesson plan snapshots with breaks preserved

**Self-Service Profile**:
- `PUT /api/auth/me` - Update own profile (email, name, password)
- `DELETE /api/auth/me` - Delete own account with cascading deletion

### Admin User Management

- `GET /api/auth/users` - List all users
- `POST /api/auth/users` - Create user with role assignment
- `PUT /api/auth/users/{id}` - Update user (prevents self-deletion)
- `DELETE /api/auth/users/{id}` - Remove user and related data

## Activity and Content Management

**Activity Discovery** (public):
- `GET /api/activities/` - Search with filters (name, age, format, bloom_level, resources, topics, duration)
- `GET /api/activities/{id}` - Retrieve single activity
- `GET /api/activities/{id}/pdf` - Download activity PDF

**Admin Content Management**:
- `POST /api/activities/create` - Create activity from previously uploaded PDF
- `POST /api/activities/upload-and-create` - Combined endpoint: upload, extract (via LLM), and create
- `DELETE /api/activities/{id}` - Remove activity

**PDF Document Operations**:
- `POST /api/documents/upload_pdf` - Upload PDF document
- `GET /api/documents/{id}` - Retrieve raw PDF
- `GET /api/documents/{id}/info` - Get document metadata
- `POST /api/documents/{id}/process` - Extract activity data via LLM

## System Information

- `GET /api/meta/field-values` - Available enum values for all form fields
- `GET /api/meta/environment` - Current environment (local, staging, production)

## Error Handling

**Standard Error Codes**: 400 (Bad Request), 401 (Unauthorised), 403 (Forbidden), 404 (Not Found), 500 (Internal Server Error)

**Error Response Format**:
```json
{
  "error": "Error message describing what went wrong"
}
```

## Implementation Details

- **Service Architecture**: See `docs/server/server-architecture.md`
- **Database Models**: `app/db/models/`
- **API Routes**: `app/api/`
- **Business Logic**: `app/services/`
- **Recommendation Engine**: `app/core/`
- **Request Validation**: `app/utils/pydantic_models.py`

## Development

**Local Development**: `make dev` (Flask development server on port 5001)  
**Testing**: `make test` (pytest with coverage)  
**API Documentation**: Visit `/api/openapi` for interactive Swagger UI
