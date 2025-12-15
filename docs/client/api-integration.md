# API Integration

## Overview

Client-side API service layer implementing type-safe communication with the server, handling token management, error resilience, and optimised data loading patterns.

## Service Architecture

- **`authService`** - JWT token management with sessionStorage integration
- **`apiService`** - Standardised request handling with automatic token refresh
- **`logger`** - Environment-aware logging for development debugging
- **`secureStorage`** - Session-based token storage with automatic cleanup

## Authentication Flow

1. **Login** - Admin password or teacher verification code authentication
2. **Token Storage** - Access/refresh tokens in sessionStorage via `secureStorage`
3. **Auto-refresh** - Automatic token refresh on 401 responses
4. **Session Management** - Automatic token cleanup when browser session ends

## API Service Patterns

### Core Service Methods

```typescript
ApiService.getRecommendations(params) -> Recommendations
ApiService.generateLessonPlan(data) -> Blob (PDF)
ApiService.getActivityFavourites() -> Activity[] (bulk fetch)
ApiService.updateProfile(data) -> User
ApiService.deleteProfile() -> Confirmation
```

### N+1 Query Optimisation

**Problem**: Individual favourite status checks for each activity in lists would require N API calls.

**Solution**: Pages fetch all favourites in a single bulk call using `getActivityFavourites()`, returning all IDs at once. Components receive pre-computed favourite status via props for O(1) lookups.

**Implementation**:
1. Page level fetches all favourites on mount → stores IDs in a Set
2. Component receives `initialIsFavourited` prop with pre-computed status
3. Lookup via Set membership test (O(1) instead of API call)
4. User actions still make individual API calls to add/remove

**Result**: Single API call replaces N+1 pattern, dramatically reducing network overhead.

## Authentication Strategy

### Token Storage

JWT tokens stored in `sessionStorage` with tokens sent via `Authorization` headers:
- **CSRF Immune**: Browsers do not automatically attach sessionStorage tokens to cross-site requests
- **Automatic Cleanup**: Session ends when browser closes, preventing token inheritance on shared devices
- **Tab Isolation**: Tokens not shared across browser tabs
- **Educational Context**: Aligns with shared lab computer usage patterns in classrooms

### Error Resilience

- **Connection Timeout** - Handles network connectivity issues
- **Manual Retry** - Users can retry failed requests via UI buttons
- **Authentication Errors** - Automatic token refresh on 401; redirect to login on refresh failure
- **PDF Upload Error Recovery** - Users can skip AI extraction while running and enter data manually; failed extractions can be retried

## Field Values Synchronisation

### Strategy

- **Client Defaults**: `src/constants/fieldValues.ts` provides instant form rendering
- **Server Authority**: `server/app/core/models.py` enum values are authoritative
- **Manual Sync**: Client and server values must be kept in sync during development
- **Fallback**: API endpoint `/api/meta/field-values` available for API-driven clients

### Field Categories

- `format` - Activity format (unplugged, digital, hybrid)
- `resources_available` - Required resources
- `bloom_level` - Bloom's Taxonomy levels
- `topics` - Computational thinking topics
- `priority_categories` - Recommendation scoring emphasis
- `mental_load`, `physical_energy` - Activity intensity levels
- `age_range` - Valid age bounds

## Environment Detection

Runtime environment fetched from `/api/meta/environment` endpoint:
- Allows same build to run across environments
- Configured via `ENVIRONMENT` variable in `.env`
- Options: local, staging, production
- Hook: `useEnvironment()` returns `{ environment, isLoading, error }`

## Data Models

### Core Types

- **Activity**: id, name, age_min, age_max, format, bloom_level, duration_min, topics, breaks
- **SearchCriteria**: target_age, format, bloom_levels, duration, topics, priority_categories
- **Recommendation**: activities (array), score (0-100), score_breakdown (per-category scores)
- **LessonPlanData**: activities (with inline breaks), total_duration, title

## Error Handling

**Automatic token refresh** on 401 with transparent session maintenance. **Clear error messages** guide user recovery. **API errors** logged with context for debugging. **Graceful degradation** when services unavailable.

## Utilities

### Secure Storage

```typescript
secureStorage.getAccessToken(): string | null
secureStorage.setTokens(accessToken, refreshToken): void
secureStorage.clearTokens(): void
```

Session-based storage with error resilience for private browsing mode.

### Logging Service

```typescript
logger.debug(message, data?, source?)
logger.info(message, data?, source?)
logger.warn(message, data?, source?)
logger.error(message, data?, source?)
```

Environment-aware: debug/info only in development; warn/error always logged.

## Testing Strategy

**Authentication Service** - Token management, API calls, error handling  
**Custom Hooks** - Form state management and validation  
**Utility Functions** - SessionStorage operations and error resilience  
**Focus**: Critical business logic and error paths, not UI rendering
