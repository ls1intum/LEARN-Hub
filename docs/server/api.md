# API Documentation

## Overview

RESTful API providing activity recommendations with scoring algorithms, lesson plan generation, and user preference tracking with dual authentication for admins and teachers.

**Base URL**: `http://localhost:5001` (development) | `https://your-domain.com` (production)  
**Documentation**: `/openapi` (Swagger UI) | `/openapi.json` (OpenAPI spec)

## Authentication

### Dual Authentication System

**Admin Access** (Content Management):
- Email/password authentication for full system access
- Activity creation, user management, PDF upload capabilities
- JWT tokens with admin role permissions

**Teacher Access** (Activity Discovery):
- Email verification codes OR password authentication
- Email verification: 6-digit codes (10-minute expiration, 3-attempt limit)
- Password login: Auto-generated secure passwords
- Lesson plan generation, favourites management, search history
- JWT tokens with teacher role permissions

### Authentication Endpoints

**Admin Login**: `POST /api/auth/admin/login`
**Teacher Login**: `POST /api/auth/login` (password-based)
**Teacher Verification**: `POST /api/auth/verification-code` → `POST /api/auth/verify` (email code)
**Teacher Registration**: `POST /api/auth/register-teacher` (auto-generates credentials)
**Password Reset**: `POST /api/auth/reset-password` (generates password)
**Token Refresh**: `POST /api/auth/refresh`
**User Info**: `GET /api/auth/me`
**Update Profile**: `PUT /api/auth/me` (self-service)
**Delete Account**: `DELETE /api/auth/me` (self-service)
**Logout**: `POST /api/auth/logout`

## Core API Patterns

### Flask-OpenAPI3 Integration

**API Framework**:
- Flask-OpenAPI3 for automatic OpenAPI 3.0 specification generation
- Type-safe request/response validation with Pydantic v2
- Automatic Swagger UI documentation at `/openapi`
- JWT Bearer authentication scheme with automatic token validation
- Error handling and validation

### Request Validation

**Pydantic Models** (`app/utils/pydantic_models.py`):
- Type-safe request validation with strict type checking
- Email format validation using `EmailStr`
- Range validation for age (6-15), duration (1-300 minutes)
- Enum validation for formats, bloom levels, energy levels
- Automatic conversion of single strings to lists for consistency

**Validation Decorators**:
- `@auth_required`: General authenticated access
- `@admin_required`: Admin-only endpoints
- Automatic Pydantic validation via Flask-OpenAPI3

## API Endpoints

### Activity Discovery (Public Access)

**Search Activities**: `GET /api/activities/`
- Query parameters: `name`, `age_min`, `age_max`, `format`, `bloom_level`, `resources_needed`, `topics`, `mental_load`, `physical_energy`, `duration_min`, `duration_max`
- Pagination: `limit`, `offset`
- Returns: List of activities with filtering and pagination

**Get Activity Details**: `GET /api/activities/{id}`
- Returns: Single activity details by ID

**Create Activity**: `POST /api/activities/create` (Admin only)
- Creates activity from uploaded PDF document
- Requires: `document_id` from PDF upload

**Delete Activity**: `DELETE /api/activities/{id}` (Admin only)

### Recommendations (Public Access with Optional Auth)

**Get Recommendations**: `GET /api/activities/recommendations`
- Query parameters: `name`, `target_age`, `format`, `available_resources`, `bloom_levels`, `preferred_topics`, `priority_categories`, `target_duration`, `include_breaks`, `max_activity_count`, `limit`
- Returns: Activity recommendations with enhanced scoring and lesson plan generation
- **Priority Categories**: Support for `age_appropriateness`, `topic_relevance`, `duration_fit`, `bloom_level_match`, `series_duration_fit` to boost scoring for specific criteria
- Authentication: Optional. If a valid Bearer token is provided, the request is associated with the user and their search query is saved to search history. If no/invalid token is provided, results are still returned but no history is saved.

**Get Scoring Insights**: `GET /api/activities/scoring-insights`
- Returns: Information about scoring categories and their impact levels

### Lesson Planning (Teacher Access)

**Generate Lesson Plan**: `POST /api/activities/lesson-plan`
- Request body: `activities`, `search_criteria`, `breaks`, `total_duration`
- Returns: `application/pdf` attachment with summary page + combined activity PDFs
- Break constraints: Breaks are allowed only between activities. A break after the final activity is automatically removed; at most `(len(activities) - 1)` breaks are included.

### User Management

**Teacher Registration** (Public): `POST /api/auth/register-teacher`
**Password Management** (Public): `POST /api/auth/reset-password`
**Search History** (Teacher Access):
- Get search history: `GET /api/history/search?limit=50&offset=0`
  - Pagination: `limit` (default 50), `offset` (default 0)
  - Returns: Array of search queries with timestamps and search criteria
  - Response: `{"search_history": [...], "pagination": {limit, offset, count}}`
- Delete search history entry: `DELETE /api/history/search/{history_id}`
  - Removes a specific search query from user's history
  - Returns: Confirmation message
- **Automatic Tracking**: Search queries are automatically saved when users make recommendations requests with valid authentication

**Favourites** (Teacher Access):
- **Individual Activities**:
  - Save favourite: `POST /api/history/favourites/activities` with `{activity_id, name?}`
  - Get favourites: `GET /api/history/favourites/activities?limit=50&offset=0`
  - Check status: `GET /api/history/favourites/activities/{activity_id}/status`
  - Remove favourite: `DELETE /api/history/favourites/activities/{activity_id}`
- **Lesson Plans**:
  - Save favourite: `POST /api/history/favourites/lesson-plans` with `{activity_ids, name?, lesson_plan}`
  - Get favourites: `GET /api/history/favourites/lesson-plans?limit=50&offset=0`
  - Delete favourite: `DELETE /api/history/favourites/{favourite_id}`
- **Response Format**:
  - Each favourite includes: `id, favourite_type ("activity" or "lesson_plan"), name, created_at`
  - Lesson plan snapshots preserve activities with breaks for accurate replay

### Self-Service Profile Management

**Update Profile**: `PUT /api/auth/me`
- Request body: `{email?, first_name?, last_name?, password?}` (all optional)
- Allows users to update their own profile information
- Email validation: Prevents duplicate emails (409 conflict if taken)
- Password change: Optional, minimum 8 characters
- Returns: Updated user object
- Access: All authenticated users (ADMIN, TEACHER)

**Delete Account**: `DELETE /api/auth/me`
- Permanently deletes the current user's account
- Cascades deletion to related data (search history, favourites)
- Returns: Confirmation message
- Access: All authenticated users (ADMIN, TEACHER)

### User Management (Admin Only)

**Get Users**: `GET /api/auth/users`
- Returns: List of all users with role and account information
- Response: `{"users": [{"id", "email", "first_name", "last_name", "role"}, ...]}`

**Create User**: `POST /api/auth/users`
- Request body: `{email, first_name, last_name, role, password}`
- Role options: "TEACHER" or "ADMIN"
- Password requirements: Minimum 8 characters
- Returns: Created user object with ID
- Email notification: Sent to teachers with their credentials

**Update User**: `PUT /api/auth/users/{user_id}`
- Request body: `{email?, first_name?, last_name?, role?, password?}` (all optional)
- Allows updating any user field individually
- Email validation: Prevents duplicate emails
- Returns: Updated user object

**Delete User**: `DELETE /api/auth/users/{user_id}`
- Prevents admin from deleting their own account
- Permanently removes user and associated data
- Returns: Confirmation message

### Content Management (Admin Only)

**PDF Upload**: `POST /api/documents/upload_pdf`
**PDF Retrieval**: `GET /api/documents/{document_id}`

### System Information (Public Access)

**Field Values**: `GET /api/meta/field-values`
- Returns: Available enum values for all form fields (format, resources, bloom_level, topics, etc.)

## Error Handling

**Standard Error Codes**: `400` (Bad Request), `401` (Unauthorized), `403` (Forbidden), `404` (Not Found), `500` (Internal Server Error)

**Error Response Format**:
```json
{
  "error": "Error message describing what went wrong"
}
```

## Core Recommendation Engine

### Enhanced Scoring System

**Category-Based Scoring**:
- Age appropriateness: Age closeness scoring
- Bloom level match: Exact and adjacent level matching
- Topic relevance: Topic overlap scoring
- Duration fit: Duration closeness scoring
- Series duration fit: Multi-activity duration optimization

**Priority Fields System**:
- Fields marked as priority receive 2x scoring multiplier
- Supported priority fields: `age_appropriateness`, `topic_relevance`, `duration_fit`, `bloom_level_match`, `series_duration_fit`

**Return Format**:
- Returns `list[tuple[list[ActivityModel | Break], ScoreModel]]`
- Each tuple represents one recommendation: `(activities_list, score_model)`
- `activities_list`: List of activities and breaks for this recommendation
- `score_model`: Detailed scoring breakdown with category scores

### Lesson Plan Generation

**Activity Series Generation**:
- Generates activity series with configurable length (1-5 activities)
- Enforces Bloom's taxonomy progression (non-decreasing)
- Supports topic overlap between consecutive activities
- Automated break insertion based on energy levels and format transitions

**Break Calculation**:
- Format transition breaks (5 minutes)
- High mental load breaks (10 minutes)
- High physical energy breaks (5 minutes)
- Prep/cleanup time integration

## Implementation Details

**Service Architecture**: See `docs/server/server-architecture.md`
**Database Models**: `app/db/models/`
**API Endpoints**: `app/api/` (Flask-OpenAPI3 routes)
**Business Logic**: `app/services/`
**Core Engine**: `app/core/engine.py` - Main recommendation algorithm
**Scoring Logic**: `app/core/scoring.py` - Individual scoring functions
**Request Validation**: `app/utils/pydantic_models.py` (Pydantic v2 models)

## Development

**Local Development**: `make dev` (Flask development server)
**Testing**: `make test` (pytest with coverage)
**Code Quality**: `make lint` (ruff), `make format` (black)
**Database**: `make db-setup` (Alembic migrations)
**API Documentation**: Visit `/openapi` for interactive Swagger UI