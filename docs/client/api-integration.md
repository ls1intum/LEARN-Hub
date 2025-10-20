# API Integration

## Overview

API service layer implementing activity recommendation system with priority scoring, field value synchronization, and role-based authentication. Focus on type safety, error resilience, and development debugging capabilities.

## Service Architecture

### Core Services
- **`authService`** - JWT token management with sessionStorage integration
- **`apiService`** - API service with standardized request handling
- **`logger`** - Environment-aware logging for development debugging
- **`secureStorage`** - Session-based token storage with automatic cleanup

### Authentication Flow
1. **Login** - Admin password or teacher verification code
2. **Token Storage** - Access/refresh tokens in sessionStorage via `secureStorage`
3. **Auto-refresh** - Automatic token refresh on 401 responses
4. **Session Management** - Automatic token cleanup when browser session ends

## API Service Patterns

### Request Handling
```typescript
// Standardized API calls via ApiService class
ApiService.request<T>(url, options) -> T // server returns data directly without a wrapper

// Key service methods
ApiService.getRecommendations(params) -> ResultsData // GET /api/activities/recommendations?{params}
ApiService.getFieldValues() -> FieldValues // GET /api/meta/field-values
ApiService.uploadPdf(file) -> UploadResponse // POST /api/documents/upload_pdf
ApiService.createActivity(data) -> ActivityResponse // POST /api/activities/create
ApiService.generateLessonPlan(data) -> Blob // POST /api/activities/lesson-plan (returns application/pdf)
```

### Type System (`types/api.ts`)
- **`ApiResponse<T>`** - Generic API response wrapper
- **`FormFieldData`** - Form field handling with proper typing
- **`SearchCriteria`** - Recommendation search criteria
- **`CreateActivityRequest`** - Activity creation request structure
- **`FavoriteActivityRequest`** - Activity favourite request
- **`LessonPlanRequest`** - Lesson plan generation request

## Key API Endpoints

### Authentication
- **`POST /api/auth/login`** - Admin login with email/password
- **`POST /api/auth/verification-code`** - Teacher verification code login
- **`GET /api/auth/me`** - Get user info

### Activities & Recommendations
- **`GET /api/activities/recommendations`** - Get activity recommendations with priority scoring
- **`POST /api/activities/lesson-plan`** - Generate lesson plan from activities
- **`POST /api/activities/create`** - Create activity (admin)
- **`GET /api/activities/{id}/pdf`** - Download activity PDF

### Favourites & History
- **`GET /api/history/favourites/activities`** - Get user's favourite activities
- **`POST /api/history/favourites/activities`** - Save activity as favourite
- **`GET /api/history/favourites/lesson-plans`** - Get user's favourite lesson plans (includes `lesson_plan` snapshot)
- **`POST /api/history/favourites/lesson-plans`** - Save lesson plan favourite with snapshot
  - Request body:
    - `activity_ids: number[]` (required)
    - `name?: string`
    - `lesson_plan: LessonPlanData` (required)
- **`GET /api/history/search`** - Get user's search history

## Data Models

### Activity
```typescript
interface Activity {
  id: number;
  name: string;
  age_min: number;
  age_max: number;
  format: string;
  bloom_level: string;
  duration_min_minutes: number;
  topics?: string[];
  type: "activity" | "break";
  break_after?: BreakAfter; // Embedded breaks
}
```

### LessonPlanData (snapshot)
```typescript
interface LessonPlanData {
  activities: Activity[]; // activities may include break_after inline
  total_duration_minutes: number;
  ordering_strategy?: string;
  title?: string;
}
```

### Recommendation Request
```typescript
interface RecommendationRequest {
  target_age?: number;
  format?: string[];
  bloom_levels?: string[];
  target_duration?: number;
  max_activity_count?: number;
  priority_categories?: string[]; // 2x scoring multiplier
  include_breaks?: boolean;
}
```

### Recommendation Response
```typescript
interface ResultsData {
  activities: Recommendation[];
  total: number;
  search_criteria: Record<string, string>;
  generated_at: string;
}

interface Recommendation {
  activities: Activity[];
  score: number; // 0-100 relevance score
  score_breakdown: Record<string, CategoryScore>;
}
```

## Error Handling Strategy

### Network Resilience
- **Connection Timeout** - Handles network connectivity issues
- **Retry Logic** - Automatic retry for transient failures
- **Break Processing Fallback** - Automatic retry without breaks when server has errors
- **Graceful Degradation** - Users get recommendations, even when breaks fail

### Authentication Errors
- **Automatic Token Refresh** - On 401 responses
- **Redirect to Login** - On refresh failure
- **Clear Error States** - On successful re-authentication

### Server Response Inconsistencies
- **Break Duration Field Fallback** - Handles `duration` vs `duration_minutes` field names
- **Field Name Priority** - `duration_minutes` > `duration` > `duration_min_minutes` > `0`

## Field Values Synchronization

### Field Values Strategy
- **Primary Source** - Client-side constants in `constants/fieldValues.ts`
- **Fallback System** - API as optional enhancement
- **Type Safety** - TypeScript types generated from field values
- **Priority Categories** - `age_appropriateness`, `bloom_level_match`, `topic_relevance`, `duration_fit`

### Field Values API
```typescript
interface FieldValues {
  format: string[];
  resources_available: string[];
  bloom_level: string[];
  topics: string[];
  priority_categories: string[];
  mental_load: string[];
  physical_energy: string[];
  age_range: { min: number; max: number };
}
```

## Utilities

### Secure Storage (`utils/secureStorage.ts`)
```typescript
secureStorage.getAccessToken(): string | null
secureStorage.setTokens(accessToken: string, refreshToken: string): void
secureStorage.clearTokens(): void
```
- **Session-based storage** - Uses sessionStorage for automatic cleanup
- **Error resilience** - Graceful handling of storage errors
- **Security enhancement** - Reduced token persistence

### Logging Service (`services/logger.ts`)
```typescript
logger.debug(message: string, data?: unknown, source?: string): void
logger.info(message: string, data?: unknown, source?: string): void
logger.warn(message: string, data?: unknown, source?: string): void
logger.error(message: string, data?: unknown, source?: string): void
```
- **Environment-aware** - Debug/info only in development mode
- **Structured logging** - Consistent message formatting with source tags
- **Production-safe** - Warn/error always logged

## Testing Strategy

### Test Coverage
- **Authentication Service** - Token management, API calls, error handling
- **Custom Hooks** - Form state management and validation
- **Utility Functions** - SessionStorage operations and error resilience

### Test Philosophy
- **High-Value Testing** - Focus on critical business logic and error paths
- **Essential Functionality** - Authentication, form state, storage utilities
- **Error Handling** - Network failures, invalid data, edge cases
- **Integration Points** - Service layer interactions

## Implementation References

For detailed implementation, see:
- **API Service**: `src/services/apiService.ts`
- **Authentication**: `src/services/authService.ts`
- **Type Definitions**: `src/types/api.ts`, `src/types/activity.ts`, `src/types/test.ts`
- **Field Values**: `src/constants/fieldValues.ts`, `src/hooks/useFieldValues.ts`
- **Utilities**: `src/utils/secureStorage.ts`, `src/services/logger.ts`
- **Testing**: `src/test/setup.ts`, `src/services/__tests__/`, `src/hooks/__tests__/`, `src/utils/__tests__/`