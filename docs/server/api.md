# API Documentation

## Overview

RESTful API providing activity recommendations with transparent scoring, lesson plan generation, and user preference tracking with dual authentication for admins and teachers.

**Base URL**: `http://localhost:5001` (development) | `https://your-domain.com` (production)  
**Documentation**: `/api/openapi/swagger` (Swagger UI) | [Hosted Swagger UI (test deployment)](https://learnhub-test.aet.cit.tum.de/api/openapi/swagger)

## Authentication

### Dual Authentication System

**Admin Access**: Email/password authentication with JWT tokens for full system access (activity creation, user management, PDF upload).

**Teacher Access**: Email verification codes (6-digit, 10-minute expiry, 3-attempt limit) OR password authentication for activity discovery, lesson planning, and favourites management.

### Core Endpoints

**Authentication**: `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/verify-code`, `POST /api/auth/request-verification-code`, `POST /api/auth/refresh`, `GET /api/auth/me`, `PUT /api/auth/me`

## API Architecture

### Spring Boot + SpringDoc Integration

- Automatic OpenAPI 3.0 specification generation via SpringDoc annotations
- Type-safe request/response validation with Jakarta Bean Validation
- Interactive Swagger UI at `/api/openapi/swagger`
- JWT authentication with Spring Security

### Request/Response Naming Convention

All API request and response fields use **camelCase** following REST API best practices:

```json
{
  "ageMin": 8,
  "ageMax": 12,
  "bloomLevel": "remember",
  "durationMinMinutes": 30
}
```

## Core API Patterns

### Resource-Based Design

Resources organised by domain:
- `/api/activities/` - Activity discovery and management
- `/api/auth/` - Authentication and user management
- `/api/history/` - User history and favourites
- `/api/documents/` - PDF upload and retrieval
- `/api/meta/` - System metadata and configuration

### Recommendation Endpoints

**GET /api/activities/recommendations**: Accepts teacher-specified criteria (`targetAge`, `format`, `bloomLevels`, `targetDuration`, `preferredTopics`, `priorityCategories`), returns ranked recommendations with detailed scoring breakdowns.

**POST /api/activities/lesson-plan**: Generates multi-activity lesson plans from activities list with optional breaks.

**GET /api/activities/scoring-insights**: Returns information about scoring categories and their impact weights.

## User Management

### Teacher-Specific Endpoints

**Search History** (authenticated teachers):
- `GET /api/history/search` - Retrieve previous searches with pagination
- `DELETE /api/history/search/{id}` - Remove search history entry

**Favourites** (authenticated teachers):
- `POST /api/history/favourites/activities`, `GET /api/history/favourites/activities`, `DELETE /api/history/favourites/activities/{id}` - Individual activity favourites
- `POST /api/history/favourites/lesson-plans`, `GET /api/history/favourites/lesson-plans`, `DELETE /api/history/favourites/{id}` - Lesson plan snapshots

**Self-Service Profile**:
- `PUT /api/auth/me` - Update own profile (email, name, password)

### Admin User Management

- `GET /api/auth/users` - List all users
- `POST /api/auth/users` - Create user with role assignment
- `PUT /api/auth/users/{id}` - Update user
- `DELETE /api/auth/users/{id}` - Remove user and related data

## Activity and Content Management

**Activity Discovery** (public):
- `GET /api/activities/` - Search with filters (`name`, `ageMin`, `ageMax`, `format`, `bloomLevel`, `resourcesNeeded`, `topics`, `durationMin`, `durationMax`)
- `GET /api/activities/{id}` - Retrieve single activity
- `GET /api/activities/{id}/pdf` - Download activity PDF

**Admin Content Management**:
- `POST /api/activities/upload-and-create-pending` - Upload PDF and create an admin draft; optional `generateContent=false` skips background generation
- `PUT /api/activities/{id}` - Update draft/activity metadata and markdowns
- `PUT /api/activities/{id}/publish` - Publish a draft activity
- `DELETE /api/activities/{id}` - Remove activity

**PDF Document Operations**:
- `GET /api/documents/{id}` - Retrieve raw PDF
- `GET /api/documents/{id}/info` - Get document metadata

## System Information

- `GET /api/meta/field-values` - Available enum values for all form fields
- `GET /api/hello` - Health check

## Error Handling

**Standard Error Codes**: 400 (Bad Request), 401 (Unauthorised), 403 (Forbidden), 404 (Not Found), 500 (Internal Server Error)

**Error Response Format**:
```json
{
  "error": "Error message describing what went wrong"
}
```

## Implementation Details

- **Controllers**: `server/src/main/java/com/learnhub/*/controller/`
- **DTOs**: `server/src/main/java/com/learnhub/*/dto/`
- **Services**: `server/src/main/java/com/learnhub/*/service/`
- **Entities**: `server/src/main/java/com/learnhub/*/entity/`

## Development

**Local Development**: `make dev` from `server/` (Spring Boot on port 5001)  
**Testing**: `make test` (JUnit with Mockito)  
**API Documentation**: Visit `/api/openapi/swagger` for interactive Swagger UI
